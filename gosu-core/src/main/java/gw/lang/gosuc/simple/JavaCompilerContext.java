package gw.lang.gosuc.simple;

import gw.lang.gosuc.cli.CommandLineOptions;

import java.util.List;

public class JavaCompilerContext extends CompilerContext<JavaCompilationResult> {
  public JavaCompilerContext(CommandLineOptions options, List<String> sourceFiles, ICompilerDriver driver) {
    super(
            driver,
            new JavaSourceCompiler(options, sourceFiles),
            new JavaCompilationDiagnosticReporter(driver),
            new JavaCompilationOutputWriter(driver));
  }
}
