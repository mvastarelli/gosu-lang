package gw.lang.gosuc.simple;

import java.io.File;

public class GosuCompilerContext extends CompilerContext<GosuCompilationResult> {
  public GosuCompilerContext(File compilingSourceFile, ICompilerDriver driver) {
    super(
            driver,
            new GosuSourceCompiler(driver, compilingSourceFile),
            new GosuCompilerDiagnosticReporter(driver, compilingSourceFile),
            new GosuCompilerOutputWriter(driver, compilingSourceFile));
  }
}
