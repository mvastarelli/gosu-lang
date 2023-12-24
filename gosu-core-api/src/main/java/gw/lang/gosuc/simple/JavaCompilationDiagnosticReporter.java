package gw.lang.gosuc.simple;

public class JavaCompilationDiagnosticReporter implements ICompilationDiagnosticReporter<JavaCompilationResult> {
  private final ICompilerDriver _driver;

  public JavaCompilationDiagnosticReporter(ICompilerDriver driver) {
    _driver = driver;
  }

  @Override
  public void report(JavaCompilationResult result) {
    var diagnostics = result.getDiagnostics();

    if(diagnostics.isEmpty()) {
      return;
    }

    diagnostics.get().forEach(_driver::sendCompileIssue);
  }
}
