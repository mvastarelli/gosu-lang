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
import gw.util.concurrent.SyncRoot;

import static gw.lang.reflect.gs.BytecodeOptions.JDWP_ENABLED;
import static java.lang.Boolean.TRUE;

public class CommonServices extends ServiceKernel implements SyncRoot.ReaderWriter
{
  // These must come first!
  private static ITypeSystem _typeSystem = new TypeLoaderAccess();  // maintained outside the kernel for perf reasons
  private static final IFileSystem _fileSystem = getDefaultFileSystemInstance(); // Currently not technically a service, since it needs to be available all the time

  private static CommonServices _kernel = new CommonServices();

  static {
    Registry.addLocationListener(e -> resetKernel());
  }

  private static synchronized void resetKernel() {
    _kernel = new CommonServices();
  }

  private CommonServices() { }

  @Override
  protected void defineServices()
  {
    try
    {
      defineService( IFileSystem.class, getDefaultFileSystemInstance() );
      defineService( IGosuInitializationHooks.class, new DefaultGosuInitializationHooks());
      defineService( IGlobalLoaderProvider.class, new DefaultGlobalLoaderProvider());
      defineService( IGosuProfilingService.class, new DefaultGosuProfilingService() );

      // These originally came from core-api.
      defineService( IEntityAccess.class, new DefaultEntityAccess() );
      defineService( ICoercionManager.class, new StandardCoercionManager() );
      defineService( IGosuParserFactory.class, new GosuParserFactoryImpl() );
      defineService( IGosuShop.class, new GosuIndustrialParkImpl() );
      defineService( IGosuLocalizationService.class, new DefaultLocalizationService() );
      defineService( IXmlSchemaCompatibilityConfig.class, new DefaultXmlSchemaCompatibilityConfig() );
      defineService( IPlatformHelper.class, new DefaultPlatformHelper() );
      defineService( IExtensionFolderLocator.class, new DefaultExtensionFolderLocator() );
      defineService( IMemoryMonitor.class, new DefaultMemoryMonitor() );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
    catch( NoClassDefFoundError e)  {
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  protected void redefineServices()
  {
    redefineServicesWithClass( Registry.instance().getCommonServiceInit() );
  }

  private static IFileSystem getDefaultFileSystemInstance() {
    return TRUE.equals(JDWP_ENABLED.get()) ?
            new FileSystemImpl(IFileSystem.CachingMode.NO_CACHING) :
            new FileSystemImpl(IFileSystem.CachingMode.FULL_CACHING);
  }

  public static IEntityAccess getEntityAccess()
  {
    return _kernel.getService( IEntityAccess.class );
  }

  public static ICoercionManager getCoercionManager()
  {
    return _kernel.getService( ICoercionManager.class );
  }

  @SuppressWarnings("UnusedDeclaration")
  public static IGosuProfilingService getGosuProfilingService()
  {
    return _kernel.getService( IGosuProfilingService.class );
  }

  public static ITypeSystem getTypeSystem()
  {
    return _typeSystem;
  }

  public static void sneakySetTypeSystem(ITypeSystem typeSystem) {
    _typeSystem = typeSystem;
  }

  public static IGosuParserFactory getGosuParserFactory()
  {
    return _kernel.getService( IGosuParserFactory.class );
  }

  public static IGosuShop getGosuIndustrialPark()
  {
    return _kernel.getService( IGosuShop.class );
  }

  public static IGosuLocalizationService getGosuLocalizationService()
  {
    return _kernel.getService( IGosuLocalizationService.class );
  }

  public static IXmlSchemaCompatibilityConfig getXmlSchemaCompatibilityConfig() {
    return _kernel.getService( IXmlSchemaCompatibilityConfig.class );
  }

  public static IPlatformHelper getPlatformHelper() {
    return _kernel.getService( IPlatformHelper.class );
  }

  public static IGosuInitializationHooks getGosuInitializationHooks() {
    return _kernel.getService( IGosuInitializationHooks.class );
  }

  public static IGlobalLoaderProvider getGlobalLoaderProvider() {
    return _kernel.getService(IGlobalLoaderProvider.class);
  }

  public static IExtensionFolderLocator getExtensionFolderLocator() {
    return _kernel.getService(IExtensionFolderLocator.class);
  }

  public static IFileSystem getFileSystem() {
    return _kernel.getService(IFileSystem.class);
  }

  public static IMemoryMonitor getMemoryMonitor() {
    return _kernel.getService(IMemoryMonitor.class);
  }

  public static CommonServices getKernel() {
    return _kernel;
  }
}
