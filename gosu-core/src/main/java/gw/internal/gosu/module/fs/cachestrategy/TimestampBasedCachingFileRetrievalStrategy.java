package gw.internal.gosu.module.fs.cachestrategy;

import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;

public class TimestampBasedCachingFileRetrievalStrategy extends CachingFileRetrievalStrategy {
  private long _lastTimestamp;

  public TimestampBasedCachingFileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    super(fileSystem, parent);
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
        getFiles().clear();
        getDirectories().clear();
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