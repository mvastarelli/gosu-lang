package gw.config;

import java.lang.reflect.InvocationTargetException;

public interface IServiceKernel {
  /**
   * Gets the service of the specified type.
   *
   * @param service The type of service to get.
   * @return the requested service or null if it was not found.
   */
  <T extends IService> T getService(Class<? extends T> service);

  /**
   * Defines a service with a default provider.  This method is intended to be called from a class that implements
   * {@link ServiceKernelInit} and that will be created and given a chance to define the service implementations
   * in this kernel.
   *
   * @param serviceClass the service to provide.
   * @param implClass the provider of this service.
   */
  <T extends IService> void defineService(Class<? extends T> serviceClass, Class<? extends T> implClass ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException;

  /**
   * Defines a service provided by this ServiceKernel
   *
   * @param service - the service to provide
   * @param defaultImplementation - the default implementation of this service
   */
  <T extends IService, Q extends T> void defineService(Class<? extends T> service, Q defaultImplementation);

  /**
   * Overrides the default implemenation of the service with a different provider.  Note that the current
   * provider cannot have been accessed (all services must be consistent during runtime.)
   *
   * @param service - the service to provide.
   * @param newProvider - the new provider of this service.
   */
  <T extends IService, Q extends T> void redefineService(Class<? extends T> service, Q newProvider);

  /**
   * Overrides the default implemenation of the service with a different provider.  Note that the current
   * provider cannot have been accessed (all services must be consistent during runtime.)
   *
   * @param service - the service to provide.
   * @param newProvider - the new provider of this service.
   */
  <T extends IService, Q extends T> void redefineService_Privileged(Class<? extends T> service, Q newProvider);

  /**
   * Redefines a service with a new provider.  This method is intended to be called from a class that implements
   * {@link ServiceKernelInit} and that will be created and given a chance to redefine the service implementations
   * in this kernel.
   *
   * @param initClassName a class name of a class that implements {@link ServiceKernelInit) and that will be created
   * and given a chance to redefine the service implementations in this kernel.
   */
  void redefineServicesWithClass( String initClassName );
}
