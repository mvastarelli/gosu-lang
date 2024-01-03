package gw.internal.gosu.module.fs;

import gw.fs.IDirectory;
import gw.fs.IDirectoryUtil;
import gw.fs.IFile;
import gw.fs.IResource;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import gw.lang.reflect.module.IFileSystem;
import manifold.util.ManExceptionUtil;

public class PathDirectoryImpl extends PathResourceImpl implements IDirectory
{
  PathDirectoryImpl(IFileSystem fileSystem, Path path )
  {
    super(fileSystem, path );
  }

  @Override
  public IDirectory dir( String relativePath )
  {
    return new PathDirectoryImpl(_fileSystem, get_Path().resolve( relativePath ) );
  }

  @Override
  public IFile file( String path )
  {
    return new PathFileImpl(_fileSystem, get_Path().resolve( path ) );
  }

  @Override
  public boolean mkdir() throws IOException
  {
    return Files.createDirectory( get_Path() ) != null;
  }

  @Override
  public List<? extends IDirectory> listDirs() {
    return list(Files::isDirectory, p -> new PathDirectoryImpl(_fileSystem, p));
  }

  @Override
  public List<? extends IFile> listFiles()
  {
    return list( e -> !Files.isDirectory( e ), p -> new PathFileImpl(_fileSystem, p));
  }

  private <E extends PathResourceImpl> List<E> list( DirectoryStream.Filter<Path> filter, Function<Path, E> creator ) {
    try( DirectoryStream<Path> stream = Files.newDirectoryStream( get_Path(), filter ) )
    {
      List<E> dirs = new ArrayList<>();
      for( Path dir: stream )
      {
        dirs.add( creator.apply( dir ) );
      }
      return dirs;
    }
    catch( Exception e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public String relativePath( IResource resource )
  {
    return IDirectoryUtil.relativePath( this, resource );
  }

  @Override
  public void clearCaches() {}

  @Override
  public boolean hasChildFile( String path )
  {
    return Files.exists( get_Path().resolve( path ) );
  }

  @Override
  public boolean isAdditional()
  {
    return false;
  }

  @Override
  public boolean exists()
  {
    return Files.isDirectory( get_Path() );
  }
}
