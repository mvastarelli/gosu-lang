package gw.config;

import gw.lang.IGosuShop;
import gw.lang.parser.ICoercionManager;
import gw.lang.parser.IGosuParserFactory;
import gw.lang.reflect.IEntityAccess;
import gw.lang.reflect.ITypeSystem;
import gw.lang.reflect.module.IFileSystem;

public interface ICommonServices {
  /*
   * Gets the underlying kernel that provides the services.
   */
  IServiceKernel getUnderlyingKernel();

  <T extends IService> T getService( Class<? extends T> service );

  default IEntityAccess getEntityAccess() {
    return getService(IEntityAccess.class);
  }

  default ICoercionManager getCoercionManager() {
    return getService(ICoercionManager.class);
  }

  default IGosuProfilingService getGosuProfilingService() {
    return getService(IGosuProfilingService.class);
  }

  default IGosuParserFactory getGosuParserFactory() {
    return getService(IGosuParserFactory.class);
  }

  default IGosuShop getGosuIndustrialPark() {
    return getService(IGosuShop.class);
  }

  default IGosuLocalizationService getGosuLocalizationService() {
    return getService(IGosuLocalizationService.class);
  }

  default IXmlSchemaCompatibilityConfig getXmlSchemaCompatibilityConfig() {
    return getService(IXmlSchemaCompatibilityConfig.class);
  }

  default IPlatformHelper getPlatformHelper() {
    return getService(IPlatformHelper.class);
  }

  default IGosuInitializationHooks getGosuInitializationHooks() {
    return getService(IGosuInitializationHooks.class);
  }

  default IGlobalLoaderProvider getGlobalLoaderProvider() {
    return getService(IGlobalLoaderProvider.class);
  }

  default IExtensionFolderLocator getExtensionFolderLocator() {
    return getService(IExtensionFolderLocator.class);
  }

  default IFileSystem getFileSystem() {
    return getService(IFileSystem.class);
  }

  default IMemoryMonitor getMemoryMonitor() {
    return getService(IMemoryMonitor.class);
  }
}
