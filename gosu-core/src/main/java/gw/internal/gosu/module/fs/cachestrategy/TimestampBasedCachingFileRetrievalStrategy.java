package gw.internal.gosu.module.fs.cachestrategy;

import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;

import java.io.File;
import java.util.Collections;

public class TimestampBasedCachingFileRetrievalStrategy extends CachingFileRetrievalStrategy {
  private long _lastTimestamp;

  public TimestampBasedCachingFileRetrievalStrategy(JavaDirectoryImpl parent) {
    super(parent);
  }

  public void clearCache() {
    super.clearCache();
    _lastTimestamp = -1;
  }

  protected void refreshIfNecessary() {
    if (_lastTimestamp == -1) {
      refreshInfo();
    } else {
      File file = getParent().toJavaFile();
      long currentTimestamp = file.lastModified();
      if (currentTimestamp == 0) {
        // If the timestamp is 0, assume it's been deleted
        _files = Collections.emptyList();
        _directories = Collections.emptyList();
      } else if (_lastTimestamp != currentTimestamp) {
        refreshInfo();
      }
    }
  }

  @Override
  protected void maybeSetTimestamp(File javaFile) {
    _lastTimestamp = javaFile.lastModified();
  }
}