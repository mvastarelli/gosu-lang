package gw.lang.gosuc.simple;

public interface ICompilationDiagnosticReporter<TResult extends CompilationResult> {
  void report(TResult result);
}

