

/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.parser.expressions;

public interface IPropertyAccessIdentifier extends IIdentifierExpression {

  IIdentifierExpression getWrappedIdentifier();

}