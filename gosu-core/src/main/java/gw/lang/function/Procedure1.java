/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.function;

public abstract class Procedure1 extends AbstractBlock implements IProcedure1 {

  public Object invokeWithArgs(Object[] args) {
    if(args.length != 1) {
      throw new IllegalArgumentException("You must pass 1 args to this block, but you passed" + args.length);
    } else {
      //noinspection unchecked
      invoke(args[0]);
      return null;
    }
  }

}
