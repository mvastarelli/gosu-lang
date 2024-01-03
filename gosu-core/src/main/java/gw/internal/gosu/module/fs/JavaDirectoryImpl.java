/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs;

import gw.config.CommonServices;
import gw.fs.IDirectory;
import gw.fs.IDirectoryUtil;
import gw.fs.IFile;
import gw.fs.IResource;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class JavaDirectoryImpl extends JavaResourceImpl implements IDirectory {

  private FileRetrievalStrategy _fileRetrievalStrategy;

  public JavaDirectoryImpl(IFileSystem fileSystem, File file, IFileSystem.CachingMode cachingMode) {
    super(fileSystem, file);
    setCachingMode(cachingMode);
  }

  public void setCachingMode(IFileSystem.CachingMode cachingMode) {
    switch (cachingMode) {
      case NO_CACHING:
        _fileRetrievalStrategy = new UncachedFileRetrievalStrategy();
        break;
      case CHECK_TIMESTAMPS:
        _fileRetrievalStrategy = new TimestampBasedCachingFileRetrievalStrategy();
        break;
      case FUZZY_TIMESTAMPS:
        _fileRetrievalStrategy = new FuzzyTimestampCachingFileRetrievalStrategy();
        break;
      case FULL_CACHING:
        _fileRetrievalStrategy = new FullyCachedFileRetrievalStrategy();
        break;
      default:
        throw new IllegalStateException("Unrecognized caching mode " + cachingMode);
    }
  }

  @Override
  public void clearCaches() {
    if (_fileRetrievalStrategy instanceof CachingFileRetrievalStrategy) {
      synchronized (FileSystemImpl.CACHED_FILE_SYSTEM_LOCK) {
        ((CachingFileRetrievalStrategy) _fileRetrievalStrategy).clearCache();
      }
    }
  }

  @Override
  public IDirectory dir(String relativePath) {
//    try {
      File subDir = new File(this._file, relativePath)/*.getCanonicalFile()*/;
    return CommonServices.INSTANCE.getFileSystem().getIDirectory(subDir);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public IFile file(String path) {
//    try {
      File subFile = new File(this._file, path)/*.getCanonicalFile()*/;
    return CommonServices.INSTANCE.getFileSystem().getIFile(subFile);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
  }

  @Override
  public boolean mkdir() {
    return _file.mkdir();
  }

  @Override
  public List<? extends IDirectory> listDirs() {
    return _fileRetrievalStrategy.listDirs();
  }

  @Override
  public List<? extends IFile> listFiles() {
    return _fileRetrievalStrategy.listFiles();
  }

  @Override
  public String relativePath(IResource resource) {
    return IDirectoryUtil.relativePath(this, resource);
  }

  @Override
  public boolean exists() {
    return _file.isDirectory();
  }

  @Override
  public boolean hasChildFile(String path) {
    return _fileRetrievalStrategy.hasChildFile(path);
  }

  @Override
  public boolean isAdditional() {
    return false;
  }

  private interface FileRetrievalStrategy {
    List<? extends IDirectory> listDirs();

    List<? extends IFile> listFiles();

    boolean hasChildFile(String path);
  }

  private class UncachedFileRetrievalStrategy implements FileRetrievalStrategy {
    @Override
    public List<? extends IDirectory> listDirs() {
      List<IDirectory> results = new ArrayList<IDirectory>();
      File[] files = _file.listFiles();
      if (files != null) {
        for (File f : _file.listFiles()) {
          if (FileSystemImpl.isDirectory(f)) {
            results.add(CommonServices.INSTANCE.getFileSystem().getIDirectory(f));
          }
        }
      }
      return results;
    }

    @Override
    public List<? extends IFile> listFiles() {
      List<IFile> results = new ArrayList<IFile>();
      File[] files = _file.listFiles();
      if (files != null) {
        for (File f : files) {
          if (!FileSystemImpl.isDirectory(f)) {
            results.add(CommonServices.INSTANCE.getFileSystem().getIFile(f));
          }
        }
      }
      return results;
    }

    @Override
    public boolean hasChildFile(String path) {
      IFile childFile = file(path);
      return childFile != null && childFile.exists();
    }
  }

  private abstract class CachingFileRetrievalStrategy implements FileRetrievalStrategy {
    protected List<IDirectory> _directories;
    protected List<IFile> _files;

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
      _files = new ArrayList<IFile>();
      _directories = new ArrayList<IDirectory>();
      File javaFile = toJavaFile();
      maybeSetTimestamp(javaFile);

      File[] files = javaFile.listFiles();
      if (files != null) {
        for (File f : files) {
          if (FileSystemImpl.isDirectory(f)) {
            _directories.add(CommonServices.INSTANCE.getFileSystem().getIDirectory(f));
          } else {
            _files.add(CommonServices.INSTANCE.getFileSystem().getIFile(f));
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
      IFile childFile = file(path);
      return childFile != null && childFile.exists();
    }

    protected abstract void refreshIfNecessary();

    protected abstract void maybeSetTimestamp(File javaFile);
  }

  private class TimestampBasedCachingFileRetrievalStrategy extends CachingFileRetrievalStrategy {
    private long _lastTimestamp;

    public void clearCache() {
      // This should always be called with the CACHED_FILE_SYSTEM_LOCK monitor already acquired
      super.clearCache();
      _lastTimestamp = -1;
    }

    protected void refreshIfNecessary() {
      if (_lastTimestamp == -1) {
        refreshInfo();
      } else {
        File file = toJavaFile();
        long currentTimestamp = file.lastModified();
        if (currentTimestamp == 0) {
          // If the timestamp is 0, assume it's been deleted
          _files = Collections.emptyList();
          _directories = Collections.emptyList();
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

  private class FuzzyTimestampCachingFileRetrievalStrategy extends CachingFileRetrievalStrategy {
    private long _lastFileTimestamp;  // in ms, absolute time
    private long _lastRefreshTimestamp; // in ms, absolute time

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
        File file = toJavaFile();
        long currentTimestamp = file.lastModified();
        if (currentTimestamp == 0) {
          // If the timestamp is 0, assume it's been deleted
          _files = Collections.emptyList();
          _directories = Collections.emptyList();
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

  private class FullyCachedFileRetrievalStrategy extends CachingFileRetrievalStrategy {
    private Set<String> fileNamesSet;

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
        IFile childFile = file(path);
        return childFile != null && childFile.exists();
      }
    }
  }
}
