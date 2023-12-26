/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.parser;

import gw.internal.gosu.parser.IGosuAnnotation;
import gw.lang.reflect.IType;
import gw.lang.reflect.gs.IGosuClass;

import java.util.List;

public interface IReducedSymbol {
  boolean isStatic();
  int getModifiers();
  List<IGosuAnnotation> getAnnotations();
  String getName();
  String getDisplayName();
  String getFullDescription();
  boolean isPrivate();
  boolean isInternal();
  boolean isProtected();
  boolean isPublic();
  boolean isAbstract();
  boolean isFinal();
  boolean isReified();
  IType getType();
  IScriptPartId getScriptPart();
  IGosuClass getGosuClass();
  boolean hasTypeVariables();
  Class<?> getSymbolClass();
  boolean isValueBoxed();
  int getIndex();
  IExpression getDefaultValueExpression();
}
