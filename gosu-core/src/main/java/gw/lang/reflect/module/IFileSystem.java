/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect.module;

import gw.config.IService;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.lang.UnstableAPI;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

@UnstableAPI
public interface IFileSystem extends IService {

  IDirectory getDirectory(File dir);

  IDirectory getDirectory(Path dir );

  IDirectory getDirectory(URL url);

  IDirectory createDir(File dir);

  IFile getFile(File file);

  IFile getFile(Path file );

  IFile getFile(URL url );

  IFile getFakeFile(URL url, IModule module);

  void setCachingMode(CachingMode cachingMode);

  void clearAllCaches();

  enum CachingMode {
    NO_CACHING,
    CHECK_TIMESTAMPS,
    FUZZY_TIMESTAMPS,
    FULL_CACHING
  }
}
