/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.lang.parser.IReducedSymbol;
import gw.lang.reflect.IMethodInfo;

public interface IReducedDelegateFunctionSymbol extends IReducedSymbol {

  IMethodInfo getTargetMethodInfo();
}
