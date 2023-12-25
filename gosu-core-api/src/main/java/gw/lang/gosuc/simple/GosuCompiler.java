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
import manifold.util.NecessaryEvilUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static gw.lang.gosuc.simple.ISourceCollector.SourceType.GOSU;
import static gw.lang.gosuc.simple.ISourceCollector.SourceType.JAVA;

public class GosuCompiler implements IGosuCompiler
{
  protected GosuInitialization _gosuInitialization;
  private final ISourceCollectorFactory _sourceCollectorFactory;

  public GosuCompiler() {
    this(DefaultSourceCollector::new);
  }

  public GosuCompiler(ISourceCollectorFactory sourceCollectorFactory) {
    _sourceCollectorFactory = sourceCollectorFactory;
  }

  @SuppressWarnings("RedundantStringFormatCall")
  @Override
  public boolean compile( CommandLineOptions options, ICompilerDriver driver )
  {
    var collector = _sourceCollectorFactory.create(options);
    var gosuFiles = collector.getByExtension(GOSU).collect(Collectors.toList());
    var javaFiles = collector.getByExtension(JAVA).collect(Collectors.toList());

    compileGosuSources( options, driver, gosuFiles );
    compileJavaSources( options, driver, javaFiles );

    if( driver.getNumErrors() > options.getMaxErrs() )
    {
      System.out.println( String.format("Error threshold of %d exceeded; aborting compilation.", options.getMaxErrs()) );
      return true;
    }
    if( !options.isNoWarn() && driver.getNumWarnings() > options.getMaxWarns() )
    {
      System.out.println( String.format("Warning threshold of %d exceeded; aborting compilation.", options.getMaxWarns()) );
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

  private void compileGosuSources( CommandLineOptions options, ICompilerDriver driver, List<String> gosuFiles ) {
    if(gosuFiles.isEmpty()) {
      return;
    }

    // TODO -- Add parallelism back in
//    gosuFiles
//            .stream()
//            .parallel()
//            .forEach( fileName -> {
    for (var fileName : gosuFiles) {
      var file = new File(fileName);
      var fileDriver = new FileCompilerDriver(driver.isEcho(), driver.isIncludeWarnings());
      var context = new GosuCompilerContext(file, fileDriver);

      if( (driver.getNumErrors() > options.getMaxErrs()) ||
          (!options.isNoWarn() && driver.getWarnings().size() > options.getMaxWarns())) {
        break;
      }

      if (options.isVerbose()) {
        System.out.println("gosuc: about to compile file: " + file);
      }

      context.compile();
      driver.aggregate(fileDriver);
    }// );
  }

  private void compileJavaSources( CommandLineOptions options, ICompilerDriver driver, List<String> javaFiles )
  {
    if( javaFiles.isEmpty() ||
        (driver.getNumErrors() > options.getMaxErrs()) ||
        (!options.isNoWarn() && driver.getWarnings().size() > options.getMaxWarns())) {
      return;
    }

    var filesDriver = new FileCompilerDriver(driver.isEcho(), driver.isIncludeWarnings());
    var context = new JavaCompilerContext(options, javaFiles, filesDriver);

    context.compile();
    driver.aggregate(filesDriver);
  }

  private static IFileSystem createFileSystemInstance()
  {
    try
    {
      var cls = Class.forName( "gw.internal.gosu.module.fs.FileSystemImpl" );
      var m = cls.getConstructor( IFileSystem.CachingMode.class );
      return (IFileSystem)m.newInstance( IFileSystem.CachingMode.FULL_CACHING );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
