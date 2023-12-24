package gw.lang.gosuc.simple;

import gw.fs.IDirectory;
import gw.lang.javac.SourceJavaFileObject;
import gw.lang.parser.GosuParserFactory;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaType;
import manifold.internal.javac.IJavaParser;
import manifold.internal.javac.InMemoryClassJavaFileObject;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class JavaCompilerContext extends CompilerContext {
  private final List<String> _sourceFiles;

  public JavaCompilerContext(List<String> sourceFiles, ICompilerDriver driver) {
    super(driver);
    _sourceFiles = sourceFiles;
  }

  public boolean compile(boolean isVerbose, boolean isNoWarn) {
    var parser = GosuParserFactory.getInterface( IJavaParser.class );
    var errorHandler = new DiagnosticCollector<JavaFileObject>();
    List<JavaFileObject> sourceFiles = _sourceFiles.stream().map( SourceJavaFileObject::new ).collect( Collectors.toList() );
    var files = parser.compile( sourceFiles, makeJavacOptions(isVerbose, isNoWarn ), errorHandler );

    errorHandler.getDiagnostics().forEach( _driver::sendCompileIssue );
    createJavaOutputFiles( files );

    return false;
  }

  private List<String> makeJavacOptions( boolean isVerbose, boolean isNoWarn )
  {
    ArrayList<String> javacOpts = new ArrayList<>();
    javacOpts.add( "-g" );
    javacOpts.add( "-source" );
    javacOpts.add( "8" );
    javacOpts.add( "-proc:none" );
    javacOpts.add( "-Xlint:unchecked" );
    javacOpts.add( "-parameters" );

    if( isVerbose )
    {
      javacOpts.add( "-verbose" );
    }

    if( isNoWarn )
    {
      javacOpts.add( "-nowarn" );
    }

    return javacOpts;
  }

  private void createJavaOutputFiles( Collection<InMemoryClassJavaFileObject> compiledJavaFiles )
  {
    IDirectory moduleOutputDirectory = TypeSystem.getGlobalModule().getOutputPath();

    if( moduleOutputDirectory == null )
    {
      throw new RuntimeException( "Can't make class file, no output path defined." );
    }

    compiledJavaFiles = compiledJavaFiles.stream().filter( e -> TypeSystem.getByFullNameIfValid( e.getClassName().replace( '$', '.' ) ) instanceof IJavaType).collect( Collectors.toList() );

    for( InMemoryClassJavaFileObject compiledJavaFile: compiledJavaFiles )
    {
      final String outRelativePath = compiledJavaFile.getClassName().replace( '.', File.separatorChar ) + ".class";
      File child = new File( moduleOutputDirectory.getPath().getFileSystemPathString() );

      child.mkdirs();

      try
      {
        for(StringTokenizer tokenizer = new StringTokenizer( outRelativePath, File.separator + "/" ); tokenizer.hasMoreTokens(); )
        {
          String token = tokenizer.nextToken();
          child = new File( child, token );

          if( !child.exists() )
          {
            if( token.endsWith( ".class" ) )
            {
              child.createNewFile();
            }
            else
            {
              child.mkdir();
            }
          }
        }

        populateJavaClassFile( child, compiledJavaFile.getBytes() );
      }
      catch( Throwable e )
      {
        _driver.sendCompileIssue(
                null,
                ERROR,
                0,
                0,
                0,
                String.format("Cannot create .class files.%n%s", Utils.getStackTrace( e ) ) );
      }
    }
  }

  private void populateJavaClassFile( File outputFile, byte[] bytes ) throws IOException
  {
    try( OutputStream out = new FileOutputStream( outputFile ) )
    {
      out.write( bytes );
      _driver.registerOutput( null, outputFile );
    }
  }
}
