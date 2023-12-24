package gw.lang.gosuc.simple;

import manifold.internal.javac.InMemoryClassJavaFileObject;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.List;

public class JavaCompilationResult extends CompilationResult<InMemoryClassJavaFileObject> {
  private final Collection<Diagnostic<? extends JavaFileObject>> _diagnostics;

  public Collection<Diagnostic<? extends  JavaFileObject>> getDiagnostics() {
    return _diagnostics;
  }

  private JavaCompilationResult(
          boolean success,
          Collection<InMemoryClassJavaFileObject> files,
          Collection<Diagnostic<? extends  JavaFileObject>> diagnostics) {
    super(success, files);
    _diagnostics = diagnostics;
  }

  public static JavaCompilationResult failed() {
    return new JavaCompilationResult(false, null, null);
  }

  public static JavaCompilationResult success(
          Collection<InMemoryClassJavaFileObject> files,
          List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    return new JavaCompilationResult(true, files, diagnostics);
  }
}
