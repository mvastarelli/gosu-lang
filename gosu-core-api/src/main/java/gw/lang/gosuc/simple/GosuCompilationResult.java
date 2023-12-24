package gw.lang.gosuc.simple;

import gw.lang.reflect.IType;

import java.util.*;

public class GosuCompilationResult extends CompilationResult<IType> {
  private GosuCompilationResult(boolean success, IType type) {
    super(success, type);
  }

  public static GosuCompilationResult failed() {
    return new GosuCompilationResult(false, null);
  }

  public static GosuCompilationResult failed(IType type) {
    return new GosuCompilationResult(false, type);
  }

  public static GosuCompilationResult success(IType type) {
    return new GosuCompilationResult(true, type);
  }
}
