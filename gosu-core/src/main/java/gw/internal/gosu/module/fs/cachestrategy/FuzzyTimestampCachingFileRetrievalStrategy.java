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

  @Override
  protected boolean shouldRefresh() {
    if (_lastFileTimestamp == -1) {
      return true;
    } else {
      var file = getParent().toJavaFile();
      var currentTimestamp = file.lastModified();
      var refreshDelta = _lastRefreshTimestamp - currentTimestamp;

      return currentTimestamp == 0 ||
              _lastFileTimestamp != currentTimestamp ||
              refreshDelta > -16 && refreshDelta < 16;
    }
  }

  @Override
  protected void refresh() {
    File file = getParent().toJavaFile();
    long currentTimestamp = file.lastModified();

    if (currentTimestamp == 0) {
      getFiles().clear();
      getDirectories().clear();
    } else {
      _lastRefreshTimestamp = System.currentTimeMillis();
      super.refresh();
    }
  }

  @Override
  protected void maybeSetTimestamp(File javaFile) {
    _lastFileTimestamp = javaFile.lastModified();
  }
}
