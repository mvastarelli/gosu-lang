/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect.gs;

import gw.lang.Deprecated;
import gw.lang.PublishInGosu;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;

@PublishInGosu
public interface IGosuObject
{
  @Deprecated(value="Use the 'typeof' operator in Gosu instead. 'obj.IntrinsicType' becomes 'typeof obj'.")
  default IType getIntrinsicType() {
    return TypeSystem.get( getClass() );
  }

  //
  // Methods cooresponding with java.lang.Object
  //

  public String toString();

  public int hashCode();

  public boolean equals( Object o );

}
