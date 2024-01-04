package gw.internal.gosu.module.fs.extractor;

import gw.fs.IDirectory;
import gw.internal.gosu.module.fs.resource.PathDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.net.URL;
import java.nio.file.Path;

public class DirectoryResourceExtractor extends ResourceExtractor<IDirectory> {
  public DirectoryResourceExtractor(IFileSystem fileSystem) {
    super(fileSystem);
  }

  @Override
  protected IDirectory fromName(IDirectory jarFS, String entryName) {
    return jarFS.dir(entryName);
  }

  @Override
  protected IDirectory fromFile(URL location) {
    return getFileSystem().getDirectory( getFileFromURL(location) );
  }

  @Override
  protected IDirectory fromURL(URL location) {
    return null;
  }

  @Override
  protected IDirectory fromPath(Path path )
  {
    return new PathDirectoryImpl(getFileSystem(), path );
  }
}
