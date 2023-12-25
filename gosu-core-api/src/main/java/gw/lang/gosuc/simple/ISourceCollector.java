package gw.lang.gosuc.simple;

import java.util.stream.Stream;

public interface ISourceCollector {
  public enum SourceType {
    GOSU,
    JAVA
  }

  Stream<String> getByExtension(SourceType sourceType);
}

