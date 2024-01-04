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

  @Override
  protected boolean shouldRefresh() {
    if (_lastTimestamp == -1) {
      return true;
    }

    var file = getParent().toJavaFile();
    var currentTimestamp = file.lastModified();

    if (currentTimestamp == 0) {
      return true;
    } else {
      return _lastTimestamp != currentTimestamp;
    }
  }

  @Override
  protected void refresh() {
    File file = getParent().toJavaFile();
    long currentTimestamp = file.lastModified();

    if (currentTimestamp == 0) {
      getFiles().clear();
      getDirectories().clear();
    }
    else {
      super.refresh();
    }
  }

  @Override
  protected void maybeSetTimestamp(File javaFile) {
    _lastTimestamp = javaFile.lastModified();
  }
}