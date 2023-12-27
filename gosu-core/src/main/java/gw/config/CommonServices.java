/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.config;

import gw.internal.gosu.DefaultLocalizationService;
import gw.internal.gosu.memory.DefaultMemoryMonitor;
import gw.internal.gosu.module.fs.FileSystemImpl;
import gw.internal.gosu.parser.*;
import gw.lang.IGosuShop;
import gw.lang.parser.ICoercionManager;
import gw.lang.parser.IGosuParserFactory;
import gw.lang.parser.StandardCoercionManager;
import gw.lang.reflect.IEntityAccess;
import gw.lang.reflect.ITypeSystem;
import gw.lang.reflect.module.IFileSystem;

import static gw.lang.reflect.gs.BytecodeOptions.JDWP_ENABLED;
import static java.lang.Boolean.TRUE;

public class CommonServices implements ICommonServices
{
  // These must come first!
  private static ITypeSystem _typeSystem = new TypeLoaderAccess();  // maintained outside the kernel for perf reasons
  private static final IFileSystem _fileSystem = getDefaultFileSystemInstance(); // Currently not technically a service, since it needs to be available all the time
  private final HotSwappable<IServiceKernel> _kernel = new HotSwappable<>(CommonServices::resetKernel);

  // Order is important.  Define this last.
  public static final ICommonServices INSTANCE = new CommonServices();

  private CommonServices() {
    Registry.addLocationListener(e -> _kernel.reset());
  }

  @Override
  public IServiceKernel getUnderlyingKernel() {
    return _kernel.get();
  }

  @Override
  public <T extends IService> T getService(Class<? extends T> service) {
    return _kernel.get().getService(service);
  }

  public static ITypeSystem getTypeSystem()
  {
    return _typeSystem;
  }

  public static void sneakySetTypeSystem(ITypeSystem typeSystem) {
    _typeSystem = typeSystem;
  }

  public static IServiceKernel getKernel() {
    return INSTANCE.getUnderlyingKernel();
  }

  @SuppressWarnings("CallToPrintStackTrace")
  private static IServiceKernel resetKernel() {
    var kernel = new ServiceKernel();

    try
    {
      kernel.defineService( IFileSystem.class, getDefaultFileSystemInstance() );
      kernel.defineService( IGosuInitializationHooks.class, new DefaultGosuInitializationHooks());
      kernel.defineService( IGlobalLoaderProvider.class, new DefaultGlobalLoaderProvider());
      kernel.defineService( IGosuProfilingService.class, new DefaultGosuProfilingService() );

      // These originally came from core-api.
      kernel.defineService( IEntityAccess.class, new DefaultEntityAccess() );
      kernel.defineService( ICoercionManager.class, new StandardCoercionManager() );
      kernel.defineService( IGosuParserFactory.class, new GosuParserFactoryImpl() );
      kernel.defineService( IGosuShop.class, new GosuIndustrialParkImpl() );
      kernel.defineService( IGosuLocalizationService.class, new DefaultLocalizationService() );
      kernel.defineService( IXmlSchemaCompatibilityConfig.class, new DefaultXmlSchemaCompatibilityConfig() );
      kernel.defineService( IPlatformHelper.class, new DefaultPlatformHelper() );
      kernel.defineService( IExtensionFolderLocator.class, new DefaultExtensionFolderLocator() );
      kernel.defineService( IMemoryMonitor.class, new DefaultMemoryMonitor() );

      kernel.redefineServicesWithClass( Registry.instance().getCommonServiceInit() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    catch( NoClassDefFoundError e)  {
      e.printStackTrace();
      throw e;
    }

    return kernel;
  }

  private static IFileSystem getDefaultFileSystemInstance() {
    return TRUE.equals(JDWP_ENABLED.get()) ?
            new FileSystemImpl(IFileSystem.CachingMode.NO_CACHING) :
            new FileSystemImpl(IFileSystem.CachingMode.FULL_CACHING);
  }
}
