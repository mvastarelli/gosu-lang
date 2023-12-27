/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.config;

import gw.lang.parser.ILanguageLevel;
import gw.util.Stack;
import manifold.util.ReflectUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ServiceKernel implements IServiceKernel
{
  private final Map<Class<? extends IService>, IService> _services = new HashMap<>();
  private final Stack<IService> _initServices = new Stack<>();

  public <T extends IService> T getService( Class<? extends T> service )
  {
    IService serviceImpl = _services.get( service );

    if( serviceImpl == null )
    {
      throw new IllegalStateException( "The service " + service.getName() + " is not provided by this ServiceKernel." );
    }

    //noinspection unchecked
    return (T)serviceImpl;
  }

  public <T extends IService, Q extends T> void redefineService(Class<? extends T> service, Q newProvider)
  {
    IService existingServiceImpl = _services.get( service );

    if( existingServiceImpl == null )
    {
      throw new IllegalArgumentException( "Service " + service.getName() + " is not defined in this ServiceKernel.");
    }

    if( existingServiceImpl.isInited() )
    {
      throw new IllegalStateException( "Service " + service.getName() + " has already been " +
                                       "initialized with the " + existingServiceImpl.getClass().getName() +
                                       " implementation");
    }

    _services.put( service, newProvider );
  }

  public <T extends IService, Q extends T> void redefineService_Privileged(Class<? extends T> service, Q newProvider)
  {
    IService existingServiceImpl = _services.get( service );

    if( existingServiceImpl == null )
    {
      throw new IllegalArgumentException( "Service " + service.getName() + " is not defined in this ServiceKernel.");
    }

    _services.put( service, newProvider );
  }

  public <T extends IService> void defineService(Class<? extends T> serviceClass, Class<? extends T> implClass ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException
  {
    T serviceImpl = (T)ReflectUtil.constructor( implClass ).newInstance();
    defineService( serviceClass, serviceImpl );
  }

  public <T extends IService, Q extends T> void defineService(Class<? extends T> service, Q defaultImplementation)
  {
    if( !service.isInterface() )
    {
      throw new IllegalArgumentException( "Services may only be defined as interfaces, and " +
                                          service.getName() +
                                          " is not an interface" );
    }

    IService existingServiceImpl = _services.get( service );

    if( existingServiceImpl != null )
    {
      throw new IllegalStateException( "Service " + service.getName() + " has already been " +
                                       "defined with the " + existingServiceImpl.getClass().getName() +
                                       " default implementation");
    }

    _services.put( service, defaultImplementation );
  }

  @SuppressWarnings("CallToPrintStackTrace")
  public void redefineServicesWithClass(String initClassName )
  {
    try
    {
      Class<?> aClass = getClass().forName( initClassName );
      ServiceKernelInit init = (ServiceKernelInit)aClass.newInstance();
      init.init( this );
    }
    catch( ClassNotFoundException e )
    {
      try {
        Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass( initClassName );
        ServiceKernelInit init = (ServiceKernelInit)aClass.newInstance();
        init.init( this );
      } catch (Exception e1) {
        e1.printStackTrace();
        throw new RuntimeException( e1 );
      }
    }
    catch (Exception e1) {
      throw new RuntimeException( e1 );
    } finally {
      ILanguageLevel.Util.reset();
    }
  }

  private <T extends IService> void detectCircularInitializationDependencies( IService service )
  {
    if( _initServices.contains( service ) )
    {
      StringBuilder sb = new StringBuilder( "Circular service initialization dependency detected : " );

      for( IService initService : _initServices)
      {
        sb.append( "\n\t" ).append( initService );
      }

      throw new IllegalStateException( sb.toString() );
    }
  }
}
