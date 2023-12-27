package gw.lang.gosuc.simple;

import gw.config.CommonServices;
import gw.config.ExecutionMode;
import gw.lang.parser.IParseIssue;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.exceptions.ParseWarning;
import gw.lang.parser.statements.IClassFileStatement;
import gw.lang.parser.statements.IClassStatement;
import gw.lang.reflect.gs.IGosuClass;

import java.io.File;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;
import static gw.lang.gosuc.simple.ICompilerDriver.WARNING;

public class GosuCompilerDiagnosticReporter implements ICompilationDiagnosticReporter<GosuCompilationResult> {
  private final ICompilerDriver _driver;
  private final File _sourceFile;

  public GosuCompilerDiagnosticReporter(ICompilerDriver driver, File sourceFile) {
    _driver = driver;
    _sourceFile = sourceFile;
  }

  @Override
  public void report(GosuCompilationResult result) {
    for(var type : result.getTypes()) {
      IParsedElement classElement = ((IGosuClass)type).getClassStatement();
      IClassFileStatement classFileStatement = ((IClassStatement)classElement).getClassFileStatement();
      classElement = classFileStatement == null ? classElement : classFileStatement;
      ExecutionMode mode = CommonServices.INSTANCE.getPlatformHelper().getExecutionMode();

      for( IParseIssue issue : classElement.getParseIssues() )
      {
        int category = issue instanceof ParseWarning ? WARNING : ERROR;
        String message = mode == ExecutionMode.IDE ? issue.getUIMessage() : issue.getConsoleMessage();
        _driver.sendCompileIssue( _sourceFile, category, issue.getTokenStart(), issue.getLine(), issue.getColumn(), message );
      }
    }
  }
}
