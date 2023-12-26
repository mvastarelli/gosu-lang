package gw.lang.gosuc.simple;

public abstract class CompilerContext<TResult extends CompilationResult> {
  protected final ICompilerDriver _driver;
  private final ISourceCompiler<TResult> _compiler;
  private final ICompilationDiagnosticReporter<TResult> _reporter;
  private final ICompilationOutputWriter<TResult> _outputWriter;

  protected CompilerContext(
          ICompilerDriver driver,
          ISourceCompiler<TResult> compiler,
          ICompilationDiagnosticReporter<TResult> reporter,
          ICompilationOutputWriter<TResult> outputWriter) {
    _driver = driver;
    _compiler = compiler;
    _reporter = reporter;
    _outputWriter = outputWriter;
  }

  public boolean compile() {
    var result = _compiler.compile();

    if(result.isSuccess()) {
      _outputWriter.createOutputFiles(result);
    }

    _reporter.report(result);

    return !result.isSuccess();
  }
}
