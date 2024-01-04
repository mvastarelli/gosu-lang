/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs.resource;

import gw.config.CommonServices;
import gw.fs.IDirectory;
import gw.fs.IDirectoryUtil;
import gw.fs.IFile;
import gw.fs.IResource;
import gw.internal.gosu.module.fs.FileSystemImpl;
import gw.internal.gosu.module.fs.cachestrategy.*;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.util.*;
import java.util.function.BiFunction;

public class JavaDirectoryImpl extends JavaResourceImpl implements IDirectory {
  private static final Map<String, BiFunction<IFileSystem, JavaDirectoryImpl, FileRetrievalStrategy>> CACHING_MODE_TO_STRATEGY = new HashMap<>();

  private FileRetrievalStrategy _fileRetrievalStrategy;

  static {
    CACHING_MODE_TO_STRATEGY.put(IFileSystem.CachingMode.NO_CACHING.name(), UncachedFileRetrievalStrategy::new);
    CACHING_MODE_TO_STRATEGY.put(IFileSystem.CachingMode.CHECK_TIMESTAMPS.name(), TimestampBasedCachingFileRetrievalStrategy::new);
    CACHING_MODE_TO_STRATEGY.put(IFileSystem.CachingMode.FUZZY_TIMESTAMPS.name(), FuzzyTimestampCachingFileRetrievalStrategy::new);
    CACHING_MODE_TO_STRATEGY.put(IFileSystem.CachingMode.FULL_CACHING.name(), FullyCachedFileRetrievalStrategy::new);
  }

  public JavaDirectoryImpl(IFileSystem fileSystem, File file, IFileSystem.CachingMode cachingMode) {
    super(fileSystem, file);
    setCachingMode(cachingMode);
  }

  public void setCachingMode(IFileSystem.CachingMode cachingMode) {
    var strategyHandler = CACHING_MODE_TO_STRATEGY.getOrDefault(cachingMode.name(), null);

    if(strategyHandler != null) {
      _fileRetrievalStrategy = strategyHandler.apply(getFileSystem(), this);
    } else {
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
      File subDir = new File(this.getFile(), relativePath);
    return getFileSystem().getDirectory(subDir);
  }

  @Override
  public IFile file(String path) {
      File subFile = new File(this.getFile(), path)/*.getCanonicalFile()*/;
    return getFileSystem().getFile(subFile);
  }

  @Override
  public boolean mkdir() {
    return getFile().mkdir();
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
    return getFile().isDirectory();
  }

  @Override
  public boolean hasChildFile(String path) {
    return _fileRetrievalStrategy.hasChildFile(path);
  }

  @Override
  public boolean isAdditional() {
    return false;
  }
}
