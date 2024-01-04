package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

public class FullyCachedFileRetrievalStrategy extends CachingFileRetrievalStrategy {
  private Set<String> fileNamesSet;

  public FullyCachedFileRetrievalStrategy(JavaDirectoryImpl parent) {
    super(parent);
  }

  @Override
  protected void refreshIfNecessary() {
    if (_files == null) {
      refreshInfo();
      fileNamesSet =_files.stream().map(IFile::getName).collect(Collectors.toSet());
    }
  }

  @Override
  protected void maybeSetTimestamp(File javaFile) {
    // Do nothing
  }

  private Set<String> fileNamesSet() {
    if (this.fileNamesSet==null) {
      refreshIfNecessary();
    }
    return this.fileNamesSet;
  }

  @Override
  public boolean hasChildFile(String path) {
    if (path.indexOf('/') == -1 && path.indexOf('\\') == -1) {
      return fileNamesSet().contains(path);
    } else {
      IFile childFile = getParent().file(path);
      return childFile != null && childFile.exists();
    }
  }
}
