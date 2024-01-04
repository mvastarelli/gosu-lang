/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs.resource;

import gw.fs.IResource;
import gw.fs.ResourcePath;
import gw.fs.IDirectory;
import gw.lang.reflect.module.IFileSystem;

import java.io.IOException;
import java.io.File;
import java.io.Serializable;
import java.net.URI;

public abstract class JavaResourceImpl implements IResource, Serializable {
  private final IFileSystem _fileSystem;
  private final File _file;

  protected JavaResourceImpl(IFileSystem fileSystem, File file) {
    _fileSystem = fileSystem;
    _file = file.getAbsoluteFile();
  }

  public IFileSystem getFileSystem() {
    return _fileSystem;
  }

  public File getFile() {
    return _file;
  }

  @Override
  public IDirectory getParent() {
    File parentFile = _file.getParentFile();
    if (parentFile == null) {
      return null;
    } else {
      return _fileSystem.getIDirectory(parentFile);
    }
  }

  @Override
  public String getName() {
    return _file.getName();
  }

  @Override
  public boolean delete() throws IOException {
    return _file.delete();
  }

  @Override
  public URI toURI() {
    return _file.toURI();
  }

  @Override
  public ResourcePath getPath() {
    return ResourcePath.parse(_file.getAbsolutePath());
  }

  @Override
  public boolean isChildOf(IDirectory dir) {
    return dir.equals(getParent());
  }

  @Override
  public boolean isDescendantOf( IDirectory dir ) {
    if ( !(dir instanceof JavaDirectoryImpl)) {
      return false;
    }

    File javadir = ( (JavaDirectoryImpl) dir ).getFile();
    File javafile = _file.getParentFile();

    while ( javafile != null ) {
      if ( javafile.equals( javadir ) ) {
        return true;
      }

      javafile = javafile.getParentFile();
    }

    return false;
  }

  @Override
  public File toJavaFile() {
    return _file;
  }

  @Override
  public boolean isJavaFile() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JavaResourceImpl) {
      return _file.equals(((JavaResourceImpl) obj)._file);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return _file.hashCode();
  }

  @Override
  public String toString() {
    return _file.toString();
  }

  @Override
  public boolean create() {
    return false;
  }

  @Override
  public boolean isInJar() {
    return false;
  }
}
