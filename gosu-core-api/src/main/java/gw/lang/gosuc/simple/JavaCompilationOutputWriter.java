package gw.lang.gosuc.simple;

import gw.fs.IDirectory;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaType;
import manifold.internal.javac.InMemoryClassJavaFileObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class JavaCompilationOutputWriter implements ICompilationOutputWriter<JavaCompilationResult> {
  private final ICompilerDriver _driver;

  public JavaCompilationOutputWriter(ICompilerDriver driver) {
    _driver = driver;
  }

  @Override
  public void createOutputFiles(JavaCompilationResult result) {
    if(result.getJavaSource().isEmpty()) {
      return;
    }

    var compiledJavaFiles = result.getJavaSource().get();

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
