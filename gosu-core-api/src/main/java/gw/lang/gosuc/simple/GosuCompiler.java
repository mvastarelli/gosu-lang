package gw.lang.gosuc.simple;

import gw.config.CommonServices;
import gw.config.IMemoryMonitor;
import gw.config.IPlatformHelper;
import gw.config.Registry;
import gw.lang.gosuc.GosucDependency;
import gw.lang.gosuc.GosucModule;
import gw.lang.gosuc.cli.CommandLineOptions;
import gw.lang.init.GosuInitialization;
import gw.lang.parser.ICoercionManager;
import gw.lang.reflect.IEntityAccess;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.module.IExecutionEnvironment;
import gw.lang.reflect.module.IFileSystem;
import gw.util.PathUtil;
import manifold.util.NecessaryEvilUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GosuCompiler implements IGosuCompiler
{
  private static final String[] SOURCE_EXTS = { ".gs", ".gsx", ".gst", ".java" };

  protected GosuInitialization _gosuInitialization;

  @Override
  public boolean compile( CommandLineOptions options, ICompilerDriver driver )
  {
    List<String> gosuFiles = new ArrayList<>();
    List<String> javaFiles = new ArrayList<>();

    for( String fileName : getSourceFiles( options ) )
    {
      if( fileName.toLowerCase().endsWith( ".java" ) )
      {
        javaFiles.add( fileName );
      }
      else
      {
        gosuFiles.add( fileName );
      }
    }

    if( !gosuFiles.isEmpty() )
    {
      if( compileGosuSources( options, driver, gosuFiles ) )
      {
        return true;
      }
    }

    if( !javaFiles.isEmpty() )
    {
      if( compileJavaSources( options, driver, javaFiles ) )
      {
        return true;
      }
    }

    return false;
  }

  private List<String> getSourceFiles( CommandLineOptions options )
  {
    List<String> sourceFiles = options.getSourceFiles();
    if( !sourceFiles.isEmpty() )
    {
      return sourceFiles;
    }

    String sourcepath = options.getSourcepath();
    if( sourcepath.isEmpty() )
    {
      return Collections.emptyList();
    }

    sourceFiles = new ArrayList<>();
    for( StringTokenizer tok = new StringTokenizer( File.pathSeparator ); tok.hasMoreTokens(); )
    {
      String path = tok.nextToken();
      Path sourcePath = PathUtil.create( path );
      addToSources( sourcePath, sourceFiles );
    }

    return sourceFiles;
  }

  private void addToSources( Path sourcePath, List<String> sourceFiles )
  {
    if( !PathUtil.exists( sourcePath ) )
    {
      return;
    }

    if( Files.isDirectory( sourcePath ) )
    {
      for( Path child : PathUtil.listFiles( sourcePath ) )
      {
        addToSources( child, sourceFiles );
      }
    }
    else
    {
      String absolutePathName = PathUtil.getAbsolutePathName( sourcePath );
      if( isSourceFile( absolutePathName ) )
      {
        sourceFiles.add( absolutePathName );
      }
    }
  }

  private boolean isSourceFile( String absolutePathName )
  {
    return Arrays.stream( SOURCE_EXTS ).anyMatch( e -> absolutePathName.toLowerCase().endsWith( e ) );
  }

  private boolean compileGosuSources( CommandLineOptions options, ICompilerDriver driver, List<String> gosuFiles )
  {
    // TODO -- Add parallelism back in
//    gosuFiles
//            .stream()
//            .parallel()
//            .forEach( fileName -> {
    for(var fileName : gosuFiles) {
              var file = new File( fileName );
              var fileDriver = new FileCompilerDriver(driver.isEcho(), driver.isIncludeWarnings());
              var context = new GosuCompilerContext(file, fileDriver);

              if( (driver.getErrors().size() > options.getMaxErrs()) ||
                  (!options.isNoWarn() && driver.getWarnings().size() > options.getMaxWarns())) {
                break;
              }

              if( options.isVerbose() )
              {
                System.out.println( "gosuc: about to compile file: " + file );
              }

              context.compile();
              driver.aggregate( fileDriver );
            }// );

      if( driver.getErrors().size() > options.getMaxErrs() )
      {
        System.out.printf( "\nError threshold of %d exceeded; aborting compilation.", options.getMaxErrs() );
        return true;
      }
      if( !options.isNoWarn() && driver.getWarnings().size() > options.getMaxWarns() )
      {
        System.out.printf( "\nWarning threshold of %d exceeded; aborting compilation.", options.getMaxWarns() );
        return true;
      }

      return false;
  }

  private boolean compileJavaSources( CommandLineOptions options, ICompilerDriver driver, List<String> javaFiles )
  {
    var filesDriver = new FileCompilerDriver(driver.isEcho(), driver.isIncludeWarnings());
    var context = new JavaCompilerContext(options, javaFiles, filesDriver);

    context.compile();
    driver.aggregate(filesDriver);

    if( driver.getErrors().size() > options.getMaxErrs() )
    {
      System.out.printf( "\nError threshold of %d exceeded; aborting compilation.", options.getMaxErrs() );
      return true;
    }
    if( !options.isNoWarn() && driver.getWarnings().size() > options.getMaxWarns() )
    {
      System.out.printf( "\nWarning threshold of %d exceeded; aborting compilation.", options.getMaxWarns() );
      return true;
    }
    return false;
  }

  @Override
  public boolean compile( File sourceFile, ICompilerDriver driver )
  {
    var context = new GosuCompilerContext(sourceFile, driver);
    return context.compile();
  }

  @Override
  public long initializeGosu( List<String> sourceFolders, List<String> classpath, List<String> backingSourcePath, String outputPath )
  {
    NecessaryEvilUtil.bypassJava9Security();

    final long start = System.currentTimeMillis();

    CommonServices.getKernel().redefineService_Privileged( IFileSystem.class, createFileSystemInstance() );
    CommonServices.getKernel().redefineService_Privileged( IMemoryMonitor.class, new CompilerMemoryMonitor() );
    CommonServices.getKernel().redefineService_Privileged( IPlatformHelper.class, new CompilerPlatformHelper() );

    if( "gw".equals( System.getProperty( "compiler.type" ) ) )
    {
      try
      {
        IEntityAccess access = (IEntityAccess)Class.forName( "gw.internal.gosu.parser.gwPlatform.GWEntityAccess" ).newInstance();
        ICoercionManager coercionManager = (ICoercionManager)Class.forName( "gw.internal.gosu.parser.gwPlatform.GWCoercionManager" ).newInstance();
        CommonServices.getKernel().redefineService_Privileged( IEntityAccess.class, access );
        CommonServices.getKernel().redefineService_Privileged( ICoercionManager.class, coercionManager );
        Registry.instance().setAllowEntityQueires( true );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }

    IExecutionEnvironment execEnv = TypeSystem.getExecutionEnvironment();
    _gosuInitialization = GosuInitialization.instance( execEnv );
    GosucModule gosucModule = new GosucModule( IExecutionEnvironment.DEFAULT_SINGLE_MODULE_NAME,
                                               sourceFolders,
                                               classpath,
                                               backingSourcePath,
                                               outputPath,
                                               Collections.<GosucDependency>emptyList(),
                                               Collections.<String>emptyList() );
    _gosuInitialization.initializeCompiler( gosucModule );

    return System.currentTimeMillis() - start;
  }

  private static IFileSystem createFileSystemInstance()
  {
    try
    {
      Class<?> cls = Class.forName( "gw.internal.gosu.module.fs.FileSystemImpl" );
      Constructor m = cls.getConstructor( IFileSystem.CachingMode.class );
      return (IFileSystem)m.newInstance( IFileSystem.CachingMode.FULL_CACHING );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public void uninitializeGosu()
  {
    TypeSystem.shutdown( TypeSystem.getExecutionEnvironment() );
    if( _gosuInitialization != null )
    {
      if( _gosuInitialization.isInitialized() )
      {
        _gosuInitialization.uninitializeCompiler();
      }
      _gosuInitialization = null;
    }
  }

  @Override
  public boolean isPathIgnored( String sourceFile )
  {
    return CommonServices.getPlatformHelper().isPathIgnored( sourceFile );
  }
}
