package gw.lang.gosuc.simple;

import manifold.internal.javac.InMemoryClassJavaFileObject;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JavaCompilationResult extends CompilationResult {
  private final Collection<InMemoryClassJavaFileObject> _files;
  private final Collection<Diagnostic<? extends JavaFileObject>> _diagnostics;

  public Optional<Collection<InMemoryClassJavaFileObject>> getJavaSource() {
    return _files == null ? Optional.empty() : Optional.of(_files);
  }

  public Optional<Collection<Diagnostic<? extends  JavaFileObject>>> getDiagnostics() {
    return _diagnostics == null ? Optional.empty() : Optional.of(_diagnostics);
  }

  private JavaCompilationResult(
          boolean success,
          Collection<InMemoryClassJavaFileObject> files,
          Collection<Diagnostic<? extends  JavaFileObject>> diagnostics) {
    super(success);
    _files = files;
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
