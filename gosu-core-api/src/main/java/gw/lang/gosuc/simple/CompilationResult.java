package gw.lang.gosuc.simple;

import gw.lang.reflect.IType;

import java.util.Optional;

public class CompilationResult {
  private final boolean _success;

  public boolean isSuccess() {
    return _success;
  }

  public CompilationResult(boolean success) {
    _success = success;
  }
}

