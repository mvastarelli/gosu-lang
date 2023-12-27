package gw.lang.gosuc.simple;

import gw.lang.reflect.IType;

import java.util.Collection;
import java.util.Collections;

public class CompilationResult<TCompilationUnit> {
  private final boolean _success;

  public boolean isSuccess() {
    return _success;
  }

  private final Collection<TCompilationUnit> _types;

  public Collection<TCompilationUnit> getTypes() {
    return _types;
  }

  public CompilationResult(boolean success) {
    this(success, Collections.emptyList());
  }

  public CompilationResult(boolean success, TCompilationUnit type) {
    _success = success;
    _types = type == null ? Collections.emptyList() : Collections.singletonList(type);
  }

  public CompilationResult(boolean success, Collection<TCompilationUnit> types) {
    _success = success;
    _types = types == null ? Collections.emptyList() : types;
  }
}
