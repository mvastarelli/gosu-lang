package gw.internal.gosu.module.fs.cachestrategy;

import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;

public class FuzzyTimestampCachingFileRetrievalStrategy extends CachingFileRetrievalStrategy {
  private long _lastFileTimestamp;  // in ms, absolute time
  private long _lastRefreshTimestamp; // in ms, absolute time

  public FuzzyTimestampCachingFileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    super(fileSystem, parent);
  }

  public void clearCache() {
    // This should always be called with the CACHED_FILE_SYSTEM_LOCK monitor already acquired
    super.clearCache();
    _lastFileTimestamp = -1;
    _lastRefreshTimestamp = -1;
  }

  protected void refreshIfNecessary() {
    if (_lastFileTimestamp == -1) {
      doRefreshImpl();
    } else {
      File file = getParent().toJavaFile();
      long currentTimestamp = file.lastModified();
      if (currentTimestamp == 0) {
        // If the timestamp is 0, assume it's been deleted
        getFiles().clear();
        getDirectories().clear();
      } else if (_lastFileTimestamp != currentTimestamp) {
        doRefreshImpl();
      } else {
        long refreshDelta = _lastRefreshTimestamp - currentTimestamp;
        if(refreshDelta > -16 && refreshDelta < 16) {
          doRefreshImpl();
        }
      }
    }
  }

  private void doRefreshImpl() {
    _lastRefreshTimestamp = System.currentTimeMillis();
    refreshInfo();
  }

  @Override
  protected void maybeSetTimestamp(File javaFile) {
    _lastFileTimestamp = javaFile.lastModified();
  }
}
