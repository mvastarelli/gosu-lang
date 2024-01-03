/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs;

import gw.config.BaseService;
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
import java.util.function.Function;

public class FileSystemImpl extends BaseService implements IFileSystem {
  private static final Set<String> FILE_SUFFIXES = new HashSet<>(Arrays.asList(
          "class",
          "eti",
          "etx",
          "gif",
          "gr",
          "grs",
          "gs",
          "gst",
          "gsx",
          "gti",
          "gx",
          "jar",
          "java",
          "pcf",
          "png",
          "properties",
          "tti",
          "ttx",
          "txt",
          "wsdl",
          "xml",
          "xsd"));

  private static final Map<String, IProtocolAdapter> _protocolAdapters = new HashMap<>();
  public static boolean USE_NEW_API = false;

  // Really gross, non-granular synchronization, but in general we shouldn't
  // be hitting this cache much after startup anyway, so it ought to not
  // turn into a perf issue
  protected static final Object CACHED_FILE_SYSTEM_LOCK = new Object();

  private final ConcurrentHashMap<File, IDirectory> _cachedDirInfo = new ConcurrentHashMap<>();
  private volatile CachingMode _cachingMode;

  private final DirectoryResourceExtractor _directoryResourceExtractor = new DirectoryResourceExtractor();
  private final FileResourceExtractor _fileResourceExtractor = new FileResourceExtractor(this);

  static {
    var adapters = ServiceLoader.load(IProtocolAdapter.class, FileSystemImpl.class.getClassLoader());

    for (IProtocolAdapter adapter : adapters) {
      for (String protocol : adapter.getSupportedProtocols()) {
        _protocolAdapters.put(protocol, adapter);
      }
    }
  }

  public FileSystemImpl(CachingMode cachingMode) {
    _cachingMode = cachingMode;
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

    return _cachedDirInfo.computeIfAbsent(normalizeFile(dir), this::createDir);
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

    return file == null ?
            null :
            new JavaFileImpl(normalizeFile(file));
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

    return _directoryResourceExtractor.getClassResource(url);
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

    return _fileResourceExtractor.getClassResource(url);
  }

  @Override
  public IFile getFakeFile(URL url, IModule module) {
    return null;
  }

  @Override
  public void setCachingMode(CachingMode cachingMode) {
    _cachingMode = cachingMode;

    _cachedDirInfo.forEachValue(1, d -> {
      if (d instanceof JavaDirectoryImpl) {
        ((JavaDirectoryImpl) d).setCachingMode(cachingMode);
      }
    });
  }

  public void clearAllCaches() {
    if (USE_NEW_API) {
      FileFactory.instance().getDefaultPhysicalFileSystem().clearAllCaches();
      return;
    }

    _cachedDirInfo.forEachValue(1, IDirectory::clearCaches);
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

  public static boolean isDirectory(File f) {
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

  private static boolean isAssumedFileSuffix(String suffix) {
    return FILE_SUFFIXES.contains(suffix);
  }

  private IDirectory createDir( File dir ) {
    // PL-21817 in OSGi/Equinox JAR could be named as "bundlefile"
    if ( (dir.getName().toLowerCase().endsWith(".jar") ||
          dir.getName().toLowerCase().endsWith(".zip") ||
          dir.getName().equals("bundlefile")) &&
            dir.isFile()) {
      return new JarFileDirectoryImpl( dir );
    } else {
      return new JavaDirectoryImpl( dir, _cachingMode );
    }
  }

  private abstract class ResourceExtractor<TResource extends IResource> {
    private final Map<String, Function<URL, TResource>> handlers = new HashMap<>();

    protected ResourceExtractor() {
      handlers.put("file", this::getFileResource);
      handlers.put("jar", this::getJarResource);
      handlers.put("http", this::getHttpResource);
    }

    public TResource getClassResource(URL url) {
      if (url == null) {
        return null;
      }

      var handler = handlers.getOrDefault(url.getProtocol(), this::getPathResource);

      return handler.apply(url);
      // throw new RuntimeException( "Unrecognized protocol: " + _url.getProtocol() );
    }

    protected abstract TResource fromURL(URL location);

    protected abstract TResource fromName(IDirectory jarFS, String entryName);

    protected abstract TResource fromFile(URL location);

    protected abstract TResource fromPath(Path path);

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

    private TResource getFileResource(URL url) {
      return fromFile(url);
    }

    private TResource getJarResource(URL url) {
      JarURLConnection urlConnection;
      URL jarFileUrl;

      try {
        urlConnection = (JarURLConnection) url.openConnection();
        jarFileUrl = urlConnection.getJarFileURL();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      var dir = new File(jarFileUrl.getFile());
      var jarFileDirectory = _cachedDirInfo.computeIfAbsent(dir, FileSystemImpl.this::createDir);

      return fromName(jarFileDirectory, urlConnection.getEntryName());
    }

    private TResource getHttpResource(URL url) {
      TResource res = fromURL(url);

      if (res != null) {
        return res;
      }

      throw new RuntimeException( "Unable to load resource from: " + url.getProtocol() );
    }

    private TResource getPathResource(URL url) {
      Path path = null;

      try {
        path = Paths.get(url.toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }

      return fromPath(path);
    }
  }

  private class FileResourceExtractor extends ResourceExtractor<IFile> {
    private final IFileSystem _fileSystem;

    public FileResourceExtractor(IFileSystem fileSystem) {
      this._fileSystem = fileSystem;
    }

    @Override
    protected IFile fromName(IDirectory jarFS, String entryName) {
      return jarFS.file(entryName);
    }

    @Override
    protected IFile fromFile(URL location) {
      return _fileSystem.getIFile( getFileFromURL(location) );
    }

    @Override
    protected IFile fromURL(URL location) {
      return new URLFileImpl(location);
    }

    @Override
    protected IFile fromPath(Path path )
    {
      return new PathFileImpl( path );
    }
  }

  private class DirectoryResourceExtractor extends ResourceExtractor<IDirectory> {
    private final IFileSystem _fileSystem;

    public DirectoryResourceExtractor() {
      this._fileSystem = FileSystemImpl.this;
    }

    @Override
    protected IDirectory fromName(IDirectory jarFS, String entryName) {
      return jarFS.dir(entryName);
    }

    @Override
    protected IDirectory fromFile(URL location) {
      return _fileSystem.getIDirectory( getFileFromURL(location) );
    }

    @Override
    protected IDirectory fromURL(URL location) {
      return null;
    }

    @Override
    protected IDirectory fromPath(Path path )
    {
      return new PathDirectoryImpl( path );
    }
  }
}
