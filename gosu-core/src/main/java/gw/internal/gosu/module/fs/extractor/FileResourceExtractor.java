package gw.internal.gosu.module.fs.extractor;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.fs.url.URLFileImpl;
import gw.internal.gosu.module.fs.resource.PathFileImpl;
import gw.lang.reflect.module.IFileSystem;

import java.net.URL;
import java.nio.file.Path;

public class FileResourceExtractor extends ResourceExtractor<IFile> {
  public FileResourceExtractor(IFileSystem fileSystem) {
    super(fileSystem);
  }

  @Override
  protected IFile fromName(IDirectory jarFS, String entryName) {
    return jarFS.file(entryName);
  }

  @Override
  protected IFile fromFile(URL location) {
    return getFileSystem().getIFile( getFileFromURL(location) );
  }

  @Override
  protected IFile fromURL(URL location) {
    return new URLFileImpl(location);
  }

  @Override
  protected IFile fromPath(Path path )
  {
    return new PathFileImpl(getFileSystem(), path );
  }
}
