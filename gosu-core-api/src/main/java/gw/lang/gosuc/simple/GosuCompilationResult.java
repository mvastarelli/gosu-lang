package gw.lang.gosuc.simple;

import gw.lang.reflect.IType;

import java.util.Optional;

public class GosuCompilationResult extends CompilationResult {
  private final IType _type;

  public Optional<IType> getType() {
    return _type == null ? Optional.empty() : Optional.of(_type);
  }

  private GosuCompilationResult(boolean success, IType type) {
    super(success);
    _type = type;
  }

  public static GosuCompilationResult failed() {
    return new GosuCompilationResult(false, null);
  }

  public static GosuCompilationResult success(IType type) {
    return new GosuCompilationResult(true, type);
  }
}

