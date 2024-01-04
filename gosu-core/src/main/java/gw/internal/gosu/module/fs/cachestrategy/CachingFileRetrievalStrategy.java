package gw.internal.gosu.module.fs.cachestrategy;

import gw.config.CommonServices;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.module.fs.FileSystemImpl;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CachingFileRetrievalStrategy extends FileRetrievalStrategy {
  protected List<IDirectory> _directories;
  protected List<IFile> _files;

  public CachingFileRetrievalStrategy(JavaDirectoryImpl parent) {
    super(parent);
  }

  public void clearCache() {
    // This should always be called with the CACHED_FILE_SYSTEM_LOCK monitor already acquired
    _directories = null;
    _files = null;
  }

  @Override
  public List<IDirectory> listDirs() {
    synchronized (FileSystemImpl.CACHED_FILE_SYSTEM_LOCK) {
      refreshIfNecessary();
      return _directories;
    }
  }

  @Override
  public List<IFile> listFiles() {
    synchronized (FileSystemImpl.CACHED_FILE_SYSTEM_LOCK) {
      refreshIfNecessary();
      return _files;
    }
  }

  protected void refreshInfo() {
    _files = new ArrayList<>();
    _directories = new ArrayList<>();
    File javaFile = getParent().toJavaFile();
    maybeSetTimestamp(javaFile);

    File[] files = javaFile.listFiles();
    if (files != null) {
      for (File f : files) {
        if (isDirectory(f)) {
          _directories.add(CommonServices.INSTANCE.getFileSystem().getDirectory(f));
        } else {
          _files.add(CommonServices.INSTANCE.getFileSystem().getFile(f));
        }
      }
    }

    if (_directories.isEmpty()) {
      _directories = Collections.emptyList();
    } else {
      ((ArrayList) _directories).trimToSize();
    }

    if (_files.isEmpty()) {
      _files = Collections.emptyList();
    } else {
      ((ArrayList) _files).trimToSize();
    }
  }

  @Override
  public boolean hasChildFile(String path) {
    IFile childFile = getParent().file(path);
    return childFile != null && childFile.exists();
  }

  protected abstract void refreshIfNecessary();

  protected abstract void maybeSetTimestamp(File javaFile);
}
