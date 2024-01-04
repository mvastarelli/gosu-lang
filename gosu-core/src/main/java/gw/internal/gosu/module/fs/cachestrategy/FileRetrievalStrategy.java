package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;

import java.util.List;

public abstract class FileRetrievalStrategy {
  private final JavaDirectoryImpl _parent;

  protected FileRetrievalStrategy(JavaDirectoryImpl parent) {
    _parent = parent;
  }

  protected JavaDirectoryImpl getParent() {
    return _parent;
  }

  public abstract List<? extends IDirectory> listDirs();

  public abstract List<? extends IFile> listFiles();

  public abstract boolean hasChildFile(String path);
}
