/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.lang.reflect.IType;

public interface IParameterizableType {

  IType[] getLoaderParameterizedTypes();

  boolean isStrictGenerics();
}
