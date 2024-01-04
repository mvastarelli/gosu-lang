package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;
import gw.util.concurrent.SyncRoot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CachingFileRetrievalStrategy extends FileRetrievalStrategy implements SyncRoot.ReaderWriter {
  private final List<IDirectory> _directories = new ArrayList<>();
  private final List<IFile> _files = new ArrayList<>();

  protected CachingFileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    super(fileSystem, parent);
  }

  public List<IDirectory> getDirectories() {
    return _directories;
  }

  public List<IFile> getFiles() {
    return _files;
  }

  public void clearCache() {
    acquireWrite(() -> {
      // This should always be called with the CACHED_FILE_SYSTEM_LOCK monitor already acquired
      _directories.clear();
      _files.clear();
    });
  }

  @Override
  public List<IDirectory> listDirs() {
    refreshIfNecessary();
    return acquireRead( () -> (List<IDirectory>)new ArrayList<>(_directories));
  }

  @Override
  public List<IFile> listFiles() {
    refreshIfNecessary();
    return acquireRead( () -> (List<IFile>)new ArrayList<>(_files));
  }

  protected void refreshIfNecessary() {
    var needsRefresh = acquireRead(this::shouldRefresh);

    if(needsRefresh) {
      acquireWrite(this::refresh);
      refresh();
    }
  }

  protected void refresh() {
    clearCache();
    File javaFile = getParent().toJavaFile();
    maybeSetTimestamp(javaFile);

    File[] files = javaFile.listFiles();
    if (files != null) {
      for (File f : files) {
        if (isDirectory(f)) {
          _directories.add(getFileSystem().getDirectory(f));
        } else {
          _files.add(getFileSystem().getFile(f));
        }
      }
    }

    if (!_directories.isEmpty()) {
      ((ArrayList<IDirectory>) _directories).trimToSize();
    }

    if (!_files.isEmpty()) {
      ((ArrayList<IFile>) _files).trimToSize();
    }
  }

  @Override
  public boolean hasChildFile(String path) {
    IFile childFile = getParent().file(path);
    return childFile != null && childFile.exists();
  }

  protected abstract boolean shouldRefresh();

  protected abstract void maybeSetTimestamp(File javaFile);
}
