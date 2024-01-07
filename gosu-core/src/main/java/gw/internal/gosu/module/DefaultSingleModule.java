/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module;

import gw.config.CommonServices;
import gw.fs.IDirectory;
import gw.internal.gosu.dynamic.DynamicTypeLoader;
import gw.internal.gosu.parser.ExecutionEnvironment;
import gw.internal.gosu.parser.TypeLoaderAccess;
import gw.internal.gosu.properties.PropertiesTypeLoader;
import gw.lang.parser.ILanguageLevel;
import gw.lang.reflect.gs.GosuClassTypeLoader;
import gw.lang.reflect.module.IExecutionEnvironment;

import java.util.List;

public class DefaultSingleModule extends GlobalModule {
  public DefaultSingleModule(ExecutionEnvironment execEnv, String name) {
    super(execEnv, name);
  }

  public DefaultSingleModule(ExecutionEnvironment execEnv) {
    super(execEnv, IExecutionEnvironment.DEFAULT_SINGLE_MODULE_NAME);
  }

  protected List<IDirectory> getAdditionalSourceRoots() {
    return CommonServices.INSTANCE.getEntityAccess().getAdditionalSourceRoots();
  }

  @Override
  protected void createStandardTypeLoaders() {
    TypeLoaderAccess.instance().pushTypeLoader(this, new GosuClassTypeLoader(this, getFileRepository()));
    if (ILanguageLevel.Util.DYNAMIC_TYPE()) {
      TypeLoaderAccess.instance().pushTypeLoader(this, new DynamicTypeLoader(this));
    }
    if (ILanguageLevel.Util.STANDARD_GOSU()) {
      TypeLoaderAccess.instance().pushTypeLoader(this, new PropertiesTypeLoader(this));
    }
    createGlobalTypeloaders();
  }

  @Override
  protected void createExtensionTypeLoaders() {
    createExtensionTypeloadersImpl();
  }
}
