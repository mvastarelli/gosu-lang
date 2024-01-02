/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs;

import gw.config.BaseService;
import gw.config.CommonServices;
import gw.fs.FileFactory;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.fs.IResource;
import gw.fs.jar.JarFileDirectoryImpl;
import gw.fs.url.URLFileImpl;
import gw.lang.reflect.module.IFileSystem;
import gw.lang.reflect.module.IModule;
import gw.lang.reflect.module.IProtocolAdapter;
import gw.util.GosuStringUtil;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystemImpl extends BaseService implements IFileSystem {
  private static final Set<String> FILE_SUFFIXES;

  private final Map<File, IDirectory> _cachedDirInfo;
  private CachingMode _cachingMode;

  private final DirectoryResourceExtractor _iDirectoryResourceExtractor;
  private final FileResourceExtractor _iFileResourceExtractor;
  private final Map<String, IProtocolAdapter> _protocolAdapters;

  public static boolean USE_NEW_API = false;

  // Really gross, non-granular synchronization, but in general we shouldn't
  // be hitting this cache much after startup anyway, so it ought to not
  // turn into a perf issue
  static final Object CACHED_FILE_SYSTEM_LOCK = new Object();

  static {
    FILE_SUFFIXES = new HashSet<String>();
    FILE_SUFFIXES.add("class");
    FILE_SUFFIXES.add("eti");
    FILE_SUFFIXES.add("etx");
    FILE_SUFFIXES.add("gif");
    FILE_SUFFIXES.add("gr");
    FILE_SUFFIXES.add("grs");
    FILE_SUFFIXES.add("gs");
    FILE_SUFFIXES.add("gst");
    FILE_SUFFIXES.add("gsx");
    FILE_SUFFIXES.add("gti");
    FILE_SUFFIXES.add("gx");
    FILE_SUFFIXES.add("jar");
    FILE_SUFFIXES.add("java");
    FILE_SUFFIXES.add("pcf");
    FILE_SUFFIXES.add("png");
    FILE_SUFFIXES.add("properties");
    FILE_SUFFIXES.add("tti");
    FILE_SUFFIXES.add("ttx");
    FILE_SUFFIXES.add("txt");
    FILE_SUFFIXES.add("wsdl");
    FILE_SUFFIXES.add("xml");
    FILE_SUFFIXES.add("xsd");
  }

  public FileSystemImpl(CachingMode cachingMode) {
    _cachedDirInfo = new HashMap<>();
    _cachingMode = cachingMode;
    _iDirectoryResourceExtractor = new DirectoryResourceExtractor();
    _iFileResourceExtractor = new FileResourceExtractor();
    _protocolAdapters = new ConcurrentHashMap<>();
    loadProtocolAdapters();
  }

  @Override
  public IDirectory getIDirectory( Path dir )
  {
    if( dir.getFileSystem() == FileSystems.getDefault() )
    {
      // for the case where the path is a JAR file, which is a "directory"
      return getIDirectory( dir.toFile() );
    }

    if( !Files.isDirectory( dir ) )
    {
      throw new IllegalArgumentException(
              "'" + dir + "' is not a directory of the '" + dir.getFileSystem() + "' file system" );
    }
    return new PathDirectoryImpl( dir );
  }

  @Override
  public IDirectory getIDirectory(File dir) {
    if (USE_NEW_API) {
      return FileFactory.instance().getIDirectory(dir);
    }

    if (dir == null) {
      return null;
    }

    dir = normalizeFile(dir);

    synchronized (CACHED_FILE_SYSTEM_LOCK) {
      IDirectory directory = _cachedDirInfo.get(dir);
      if (directory == null) {
        directory = createDir( dir );
        _cachedDirInfo.put( dir, directory );
      }
      return directory;
    }
  }

  @Override
  public IFile getIFile( Path path )
  {
    if( path.getFileSystem() == FileSystems.getDefault() )
    {
      // for the case where the path is a normal file
      return getIFile( path.toFile() );
    }

    if( Files.isDirectory( path ) )
    {
      throw new IllegalArgumentException(
              "'" + path + "' is not a file of the '" + path.getFileSystem() + "' file system" );
    }

    return new PathFileImpl( path );
  }

  @Override
  public IFile getIFile(File file) {
    if (USE_NEW_API) {
      return FileFactory.instance().getIFile(file);
    }

    if (file == null) {
      return null;
    } else {
      return new JavaFileImpl( normalizeFile( file ) );
    }
  }

  @Override
  public IDirectory getIDirectory(URL url) {
    if (url == null) {
      return null;
    }

    IProtocolAdapter protocolAdapter = _protocolAdapters.get(url.getProtocol());
    if (protocolAdapter != null) {
      return protocolAdapter.getIDirectory(url);
    }

    return _iDirectoryResourceExtractor.getClassResource(url);
  }

  @Override
  public IFile getIFile( URL url ) {
    if (url == null) {
      return null;
    }

    IProtocolAdapter protocolAdapter = _protocolAdapters.get(url.getProtocol());
    if (protocolAdapter != null) {
      return protocolAdapter.getIFile(url);
    }

    if (USE_NEW_API) {
      return FileFactory.instance().getIFile(url);
    }
    return _iFileResourceExtractor.getClassResource(url);
  }

  @Override
  public IFile getFakeFile(URL url, IModule module) {
    return null;
  }

  @Override
  public void setCachingMode(CachingMode cachingMode) {
    synchronized (CACHED_FILE_SYSTEM_LOCK) {
      _cachingMode = cachingMode;

      for (IDirectory dir : _cachedDirInfo.values()) {
        if (dir instanceof JavaDirectoryImpl) {
          ((JavaDirectoryImpl) dir).setCachingMode(cachingMode);
        }
      }
    }
  }

  public void clearAllCaches() {
    if (USE_NEW_API) {
      FileFactory.instance().getDefaultPhysicalFileSystem().clearAllCaches();
      return;
    }

    synchronized (CACHED_FILE_SYSTEM_LOCK) {
      for (IDirectory dir : _cachedDirInfo.values()) {
        dir.clearCaches();
      }
    }
  }

  public static File normalizeFile(File file) {
    String absolutePath = file.getAbsolutePath();
    List<String> components = new ArrayList<String>();
    boolean reallyNormalized = false;
    int lastIndex = 0;

    for (int i = 0; i < absolutePath.length(); i++) {
      char c = absolutePath.charAt(i);

      if (c == '/' || c == '\\') {
        String component = absolutePath.substring(lastIndex, i);

        if (component.equals(".")) {
          reallyNormalized = true;
        } else if (component.equals("..")) {
          components.remove(components.size() - 1);
          reallyNormalized = true;
        } else {
          components.add(component);
        }

        lastIndex = i + 1;
      }
    }

    String component = absolutePath.substring(lastIndex);

    if (component.equals(".")) {
      reallyNormalized = true;
    } else if (component.equals("..")) {
      components.remove(components.size() - 1);
      reallyNormalized = true;
    } else {
      components.add(component);
    }

    return reallyNormalized ? new File(GosuStringUtil.join(components, "/")) : file;
  }

  static boolean isDirectory(File f) {
    String name = f.getName();

    if (isAssumedFileSuffix(getFileSuffix(name))) {
      return false;
    } else {
      return f.isDirectory();
    }
  }

  private static String getFileSuffix(String name) {
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex == -1) {
      return null;
    } else {
      return name.substring(dotIndex + 1);
    }
  }

  private IDirectory createDir( File dir ) {
    // PL-21817 in OSGi/Equinox JAR could be named as "bundlefile"
    if ( (dir.getName().toLowerCase().endsWith(".jar") || dir.getName().toLowerCase().endsWith(".zip") || dir.getName().equals("bundlefile")) && dir.isFile()) {
      return new JarFileDirectoryImpl( dir );
    } else {
      return new JavaDirectoryImpl( dir, _cachingMode );
    }
  }

  private void loadProtocolAdapters() {
    ServiceLoader<IProtocolAdapter> adapters = ServiceLoader.load(IProtocolAdapter.class, getClass().getClassLoader());
    for (IProtocolAdapter adapter : adapters) {
      for (String protocol : adapter.getSupportedProtocols()) {
        _protocolAdapters.put(protocol, adapter);
      }
    }
  }

  private abstract class ResourceExtractor<TResource extends IResource> {
    TResource getClassResource(URL _url) {
      if (_url == null) {
        return null;
      }

      switch (_url.getProtocol()) {
        case "file":
          return getResourceFromJavaFile(_url);
        case "jar":
          JarURLConnection urlConnection;
          URL jarFileUrl;

          try {
            urlConnection = (JarURLConnection) _url.openConnection();
            jarFileUrl = urlConnection.getJarFileURL();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          File dir = new File(jarFileUrl.getFile());
          IDirectory jarFileDirectory;

          synchronized (CACHED_FILE_SYSTEM_LOCK) {
            jarFileDirectory = _cachedDirInfo.get(dir);

            if (jarFileDirectory == null) {
              jarFileDirectory = createDir(dir);
              _cachedDirInfo.put(dir, jarFileDirectory);
            }
          }

          return getResourceFromJarDirectoryAndEntryName(jarFileDirectory, urlConnection.getEntryName());
        case "http":
          TResource res = getResourceFromURL(_url);

          if (res != null) {
            return res;
          }

          break;
        default:
          try {
            // Handle custom file systems e.g., jrt:/
            Path path = Paths.get(_url.toURI());
            return getResourceFromPath(path);
          } catch (Exception ignore) {
          }

          break;
      }

      throw new RuntimeException( "Unrecognized protocol: " + _url.getProtocol() );
    }

    abstract TResource getResourceFromURL(URL location);

    abstract TResource getResourceFromJarDirectoryAndEntryName(IDirectory jarFS, String entryName);

    abstract TResource getResourceFromJavaFile(URL location);

    abstract TResource getResourceFromPath(Path path);

    protected File getFileFromURL(URL url) {
      try {
        URI uri = url.toURI();

        if ( uri.getFragment() != null ) {
          uri = new URI( uri.getScheme(), uri.getSchemeSpecificPart(), null );
        }

        return new File( uri );
      }
      catch ( URISyntaxException ex ) {
        throw new RuntimeException( ex );
      }
      catch ( IllegalArgumentException ex ) {
        // debug getting IAE only in TH - unable to parse URL with fragment identifier
        throw new IllegalArgumentException( "Unable to parse URL " + url.toExternalForm(), ex );
      }
    }
  }

  private class FileResourceExtractor extends ResourceExtractor<IFile> {

    IFile getResourceFromJarDirectoryAndEntryName(IDirectory jarFS, String entryName) {
      return jarFS.file(entryName);
    }

    IFile getResourceFromJavaFile(URL location) {
      return CommonServices.INSTANCE.getFileSystem().getIFile( getFileFromURL(location) );
    }

    @Override
    IFile getResourceFromURL(URL location) {
      return new URLFileImpl(location);
    }

    @Override
    IFile getResourceFromPath(Path path )
    {
      return new PathFileImpl( path );
    }
  }

  private class DirectoryResourceExtractor extends ResourceExtractor<IDirectory> {

    protected IDirectory getResourceFromJarDirectoryAndEntryName(IDirectory dir, String entryName) {
      return dir.dir(entryName);
    }

    protected IDirectory getResourceFromJavaFile(URL location) {
      return CommonServices.INSTANCE.getFileSystem().getIDirectory( getFileFromURL(location) );
    }

    @Override
    IDirectory getResourceFromURL(URL location) {
      return null;
    }

    @Override
    IDirectory getResourceFromPath(Path path )
    {
      return new PathDirectoryImpl( path );
    }
  }

  private static boolean isAssumedFileSuffix(String suffix) {
    return FILE_SUFFIXES.contains(suffix);
  }
}
