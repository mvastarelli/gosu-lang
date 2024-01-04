package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

public class FullyCachedFileRetrievalStrategy extends CachingFileRetrievalStrategy {
  private Set<String> fileNamesSet;

  public FullyCachedFileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    super(fileSystem, parent);
  }

  @Override
  protected boolean shouldRefresh() {
    return getFiles().isEmpty();
  }

  @Override
  protected void refresh() {
    super.refresh();
    fileNamesSet = getFiles()
            .stream()
            .map(IFile::getName)
            .collect(Collectors.toSet());
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
