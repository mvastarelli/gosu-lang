package gw.lang.gosuc.simple;

import gw.config.CommonServices;
import gw.config.ExecutionMode;
import gw.fs.FileFactory;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.lang.init.GosuInitialization;
import gw.lang.parser.IParseIssue;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.exceptions.ParseWarning;
import gw.lang.parser.statements.IClassFileStatement;
import gw.lang.parser.statements.IClassStatement;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.ISourceFileHandle;
import gw.lang.reflect.module.IModule;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;
import static gw.lang.gosuc.simple.ICompilerDriver.WARNING;

public class GosuCompilerContext extends CompilerContext {
  private final GosuInitialization _gosuInitialization;
  private final File _sourceFile;

  public GosuCompilerContext(GosuInitialization gosuInitialization, File compilingSourceFile, ICompilerDriver driver) {
    super(driver);
    _gosuInitialization = gosuInitialization;
    _sourceFile = compilingSourceFile;
  }

  public boolean compile() {
    IType type = getType( _sourceFile );
    if( type == null )
    {
      _driver.sendCompileIssue( _sourceFile, ERROR, 0, 0, 0, "Cannot find type in the Gosu Type System." );
      return false;
    }

    if( isCompilable( type ) )
    {
      try
      {
        if( type.isValid() )
        {
          createGosuOutputFiles( (IGosuClass)type );
        }
      }
      catch( CompilerDriverException ex )
      {
        _driver.sendCompileIssue( _sourceFile, ERROR, 0, 0, 0, ex.getMessage() );
        return false;
      }

      // output warnings and errors - whether the type was valid or not
      IParsedElement classElement = ((IGosuClass)type).getClassStatement();
      IClassFileStatement classFileStatement = ((IClassStatement)classElement).getClassFileStatement();
      classElement = classFileStatement == null ? classElement : classFileStatement;
      ExecutionMode mode = CommonServices.getPlatformHelper().getExecutionMode();
      for( IParseIssue issue : classElement.getParseIssues() )
      {
        int category = issue instanceof ParseWarning ? WARNING : ERROR;
        String message = mode == ExecutionMode.IDE ? issue.getUIMessage() : issue.getConsoleMessage();
        _driver.sendCompileIssue( _sourceFile, category, issue.getTokenStart(), issue.getLine(), issue.getColumn(), message );
      }
    }

    return false;
  }

  private IType getType( File file )
  {
    IFile ifile = FileFactory.instance().getIFile( file );
    IModule module = TypeSystem.getGlobalModule();
    String[] typesForFile = TypeSystem.getTypesForFile( module, ifile );
    if( typesForFile.length != 0 )
    {
      return TypeSystem.getByFullNameIfValid( typesForFile[0], module );
    }
    return null;
  }

  private boolean isCompilable( IType type )
  {
    IType doNotVerifyAnnotation = TypeSystem.getByFullNameIfValid( "gw.testharness.DoNotVerifyResource" );
    return type instanceof IGosuClass && !type.getTypeInfo().hasAnnotation( doNotVerifyAnnotation );
  }

  private void createGosuOutputFiles( IGosuClass gsClass )
  {
    IDirectory moduleOutputDirectory = TypeSystem.getGlobalModule().getOutputPath();
    if( moduleOutputDirectory == null )
    {
      throw new RuntimeException( "Can't make class file, no output path defined." );
    }

    final String outRelativePath = gsClass.getName().replace( '.', File.separatorChar ) + ".class";
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

      populateGosuClassFile( child, gsClass );
      maybeCopySourceFile( child.getParentFile(), gsClass, _sourceFile );
    }
    catch( Throwable e )
    {
      _driver.sendCompileIssue(
              _sourceFile,
              ERROR,
              0,
              0, 0,
              String.format( "Cannot create .class files.%n%s", Utils.getStackTrace( e ) ) );
    }
  }

  private void populateGosuClassFile( File outputFile, IGosuClass gosuClass) throws IOException {
    final byte[] bytes = TypeSystem.getGosuClassLoader().getBytes(gosuClass);
    try (OutputStream out = new FileOutputStream(outputFile)) {
      out.write(bytes);
      _driver.registerOutput(_sourceFile, outputFile);
    }
    for (IGosuClass innerClass : gosuClass.getInnerClasses()) {
      final String innerClassName = String.format("%s$%s.class", outputFile.getName().substring(0, outputFile.getName().lastIndexOf('.')), innerClass.getRelativeName());
      File innerClassFile = new File(outputFile.getParent(), innerClassName);
      if (innerClassFile.isFile()) {
        innerClassFile.createNewFile();
      }
      populateGosuClassFile(innerClassFile, innerClass);
    }
  }

  private void maybeCopySourceFile(File parent, IGosuClass gsClass, File sourceFile)
  {
    ISourceFileHandle sfh = gsClass.getSourceFileHandle();
    IFile srcFile = sfh.getFile();
    if( srcFile != null )
    {
      File file = new File( srcFile.getPath().getFileSystemPathString() );
      if( file.isFile() )
      {
        try
        {
          File destFile = new File( parent, file.getName() );
          copyFile( file, destFile );
          _driver.registerOutput( _sourceFile, destFile );
        }
        catch( IOException e )
        {
          e.printStackTrace();
          _driver.sendCompileIssue( sourceFile, ERROR, 0, 0, 0, "Cannot copy source file to output folder." );
        }
      }
    }
  }

  public void copyFile( File sourceFile, File destFile ) throws IOException
  {
    if( sourceFile.isDirectory() )
    {
      destFile.mkdirs();
      return;
    }

    if( !destFile.exists() )
    {
      destFile.getParentFile().mkdirs();
      destFile.createNewFile();
    }

    try(FileChannel source = new FileInputStream( sourceFile ).getChannel();
        FileChannel destination = new FileOutputStream( destFile ).getChannel() )
    {
      destination.transferFrom( source, 0, source.size() );
    }
  }
}
