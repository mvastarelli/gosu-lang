package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UncachedFileRetrievalStrategy extends FileRetrievalStrategy {
  public UncachedFileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    super(fileSystem, parent);
  }

  @Override
  public List<? extends IDirectory> listDirs() {
    List<IDirectory> results = new ArrayList<IDirectory>();
    File[] files = getParent().getFile().listFiles();
    if (files != null) {
      for (File f : getParent().getFile().listFiles()) {
        if (isDirectory(f)) {
          results.add(getFileSystem().getDirectory(f));
        }
      }
    }
    return results;
  }

  @Override
  public List<? extends IFile> listFiles() {
    List<IFile> results = new ArrayList<IFile>();
    File[] files = getParent().getFile().listFiles();
    if (files != null) {
      for (File f : files) {
        if (!isDirectory(f)) {
          results.add(getFileSystem().getFile(f));
        }
      }
    }
    return results;
  }

  @Override
  public boolean hasChildFile(String path) {
    IFile childFile = getParent().file(path);
    return childFile != null && childFile.exists();
  }
}
