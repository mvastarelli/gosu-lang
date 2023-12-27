package gw.lang.gosuc.simple;

import gw.util.concurrent.SyncRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AggregateCompilerDriver implements ICompilerDriver, SyncRoot.ReaderWriter {
  private final boolean _echo;
  private final boolean _includeWarnings;

  private final List<String> _errors = new ArrayList<>();
  private final List<String> _warnings = new ArrayList<>();

  public AggregateCompilerDriver() {
    this(false, true);
  }

  public AggregateCompilerDriver( boolean echo, boolean warnings ) {
    _echo = echo;
    _includeWarnings = warnings;
  }

  @Override
  public void sendCompileIssue(File file, int category, long offset, long line, long column, String message) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerOutput(File sourceFile, File outputFile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isIncludeWarnings() {
    return _includeWarnings;
  }

  @Override
  public boolean isEcho() {
    return _echo;
  }

  @Override
  public boolean hasErrors() {
    return acquireRead( () -> !_errors.isEmpty() );
  }

  @Override
  public List<String> getErrors() {
    return acquireRead( () -> new ArrayList<>( _errors ) );
  }

  @Override
  public List<String> getWarnings() {
    return acquireRead( () -> new ArrayList<>( _warnings ) );
  }

  @Override
  public int getNumErrors() { return acquireRead(_errors::size); }

  @Override
  public int getNumWarnings() { return acquireRead(_warnings::size); }

  @Override
  public void aggregate( ICompilerDriver other ) {
    acquireWrite( () -> {
      _errors.addAll( other.getErrors() );
      _warnings.addAll( other.getWarnings() );
    });
  }
}
