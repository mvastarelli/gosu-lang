package gw.lang.gosuc.simple;

public abstract class CompilerContext {
  protected final ICompilerDriver _driver;
  private final ISourceCompiler _compiler;
  private final ICompilationDiagnosticReporter _reporter;
  private final ICompilationOutputWriter _outputWriter;

  protected CompilerContext(ICompilerDriver driver, ISourceCompiler compiler, ICompilationDiagnosticReporter reporter, ICompilationOutputWriter outputWriter) {
    _driver = driver;
    _compiler = compiler;
    _reporter = reporter;
    _outputWriter = outputWriter;
  }

//  public boolean compile() {
//    return compile(false, false);
//  }
//
//  public boolean compile(boolean isVerbose, boolean isNoWarn) {
//    var result = _compiler.compile();
//    _reporter.report(result);
//    _outputWriter.createOutputFiles(result);
//
//    return !result.isSuccess();
//  }
}
