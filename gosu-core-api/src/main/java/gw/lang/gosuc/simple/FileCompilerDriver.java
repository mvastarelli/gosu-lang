package gw.lang.gosuc.simple;

import javax.tools.Diagnostic;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileCompilerDriver implements ICompilerDriver{
  private final boolean _echo;
  private final boolean _includeWarnings;
  private List<String> _errors = new ArrayList<>();
  private List<String> _warnings = new ArrayList<>();

  public FileCompilerDriver() {
    this( false, true );
  }

  public FileCompilerDriver( boolean echo, boolean warnings ) {
    _echo = echo;
    _includeWarnings = warnings;
  }

  @Override
  public void sendCompileIssue(File file, int category, long offset, long line, long column, String message) {
    sendCompileIssue( (Object)file, category, offset, line, column, message );
  }

  @Override
  public void sendCompileIssue(Object file, int category, long offset, long line, long column, String message) {
    if (category == WARNING) {
      String warning = String.format( "%s:[%s,%s] warning: %s", file.toString(), line, column, message );
      _warnings.add( warning );
      if( _echo && _includeWarnings ) {
        System.out.println( warning );
      }
    } else if (category == ERROR) {
      String error = String.format( "%s:[%s,%s] error: %s", file.toString(), line, column, message );
      _errors.add( error );
      if( _echo ) {
        System.out.println( error );
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void sendCompileIssue( Diagnostic d )
  {
    sendCompileIssue( d.getSource(),
            d.getKind() == Diagnostic.Kind.ERROR ? ICompilerDriver.ERROR : ICompilerDriver.WARNING,
            d.getStartPosition(),
            d.getLineNumber(),
            d.getColumnNumber(),
            d.getMessage( Locale.getDefault() ) );
  }

  @Override
  public void registerOutput(File sourceFile, File outputFile) { }

  @Override
  public boolean isEcho() { return _echo; }

  @Override
  public boolean isIncludeWarnings()
  {
    return _includeWarnings;
  }

  @Override
  public boolean hasErrors() {
    return !_errors.isEmpty();
  }

  @Override
  public List<String> getErrors() {
    return _errors;
  }

  @Override
  public List<String> getWarnings() {
    return _warnings;
  }

  @Override
  public int getNumErrors() { return _errors.size(); }

  @Override
  public int getNumWarnings() { return _warnings.size(); }
}
