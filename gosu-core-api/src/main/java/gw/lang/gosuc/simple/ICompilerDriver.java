package gw.lang.gosuc.simple;

import java.io.File;
import java.util.List;
import javax.tools.Diagnostic;

/**
 * @author dpetrusca
 */
public interface ICompilerDriver {
  int ERROR = 0;
  int WARNING = 1;

  void sendCompileIssue(File file, int category, long offset, long line, long column, String message);

  default void sendCompileIssue(Object file, int category, long offset, long line, long column, String message)
  {
    sendCompileIssue( (File)file, category, offset, line, column, message );
  }

  default void sendCompileIssue( Diagnostic<?> d )
  {
  }

  void registerOutput(File sourceFile, File outputFile);
  
  default void registerOutput(Object sourceFile, File outputFile) 
  {
    registerOutput( (File) sourceFile, outputFile);
  }

  default boolean isIncludeWarnings()
  {
    throw new UnsupportedOperationException("isIncludeWarnings");
  }

  default boolean hasErrors() { throw new UnsupportedOperationException("hasErrors"); }

  default List<String> getErrors() 
  {
    throw new UnsupportedOperationException("getErrors");
  }

  default List<String> getWarnings() 
  {
    throw new UnsupportedOperationException("getWarnings");
  }

  // New Methods

  default void aggregate( ICompilerDriver other ) { throw new UnsupportedOperationException("aggregate"); }

  default boolean isEcho() { throw new UnsupportedOperationException("isEcho"); }
}
