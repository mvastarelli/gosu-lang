/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.module.fs;

import gw.config.BaseService;
import gw.fs.FileFactory;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.fs.jar.JarFileDirectoryImpl;
import gw.internal.gosu.module.fs.extractor.DirectoryResourceExtractor;
import gw.internal.gosu.module.fs.extractor.FileResourceExtractor;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.internal.gosu.module.fs.resource.JavaFileImpl;
import gw.internal.gosu.module.fs.resource.PathDirectoryImpl;
import gw.internal.gosu.module.fs.resource.PathFileImpl;
import gw.lang.reflect.module.IFileSystem;
import gw.lang.reflect.module.IModule;
import gw.lang.reflect.module.IProtocolAdapter;
import gw.util.GosuStringUtil;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
  public static final Object CACHED_FILE_SYSTEM_LOCK = new Object();

  private final ConcurrentHashMap<File, IDirectory> _cache = new ConcurrentHashMap<>();
  private volatile CachingMode _cachingMode;

  private final DirectoryResourceExtractor _directoryResourceExtractor = new DirectoryResourceExtractor(this);
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
  public IDirectory getIDirectory( Path dir ) {
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

    return new PathDirectoryImpl(this,  dir );
  }

  @Override
  public IDirectory getIDirectory(File dir) {
    if (USE_NEW_API) {
      return FileFactory.instance().getIDirectory(dir);
    }

    if (dir == null) {
      return null;
    }

    return _cache.computeIfAbsent(normalizeFile(dir), this::createDir);
  }

  @Override
  public IFile getIFile( Path path ) {
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

    return new PathFileImpl(this, path );
  }

  @Override
  public IFile getIFile(File file) {
    if (USE_NEW_API) {
      return FileFactory.instance().getIFile(file);
    }

    return file == null ?
            null :
            new JavaFileImpl(this, normalizeFile(file));
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

    _cache.forEachValue(1, d -> {
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

    _cache.forEachValue(1, IDirectory::clearCaches);
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

  @Override
  public IDirectory createDir( File dir ) {
    // PL-21817 in OSGi/Equinox JAR could be named as "bundlefile"
    if ( (dir.getName().toLowerCase().endsWith(".jar") ||
          dir.getName().toLowerCase().endsWith(".zip") ||
          dir.getName().equals("bundlefile")) &&
            dir.isFile()) {
      return new JarFileDirectoryImpl( dir );
    } else {
      return new JavaDirectoryImpl(this, dir, _cachingMode );
    }
  }
}
