package gw.internal.gosu.module.fs.extractor;

import gw.fs.IDirectory;
import gw.fs.IResource;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class ResourceExtractor<TResource extends IResource> {
  private final Map<String, Function<URL, TResource>> handlers = new HashMap<>();
  private final IFileSystem _fileSystem;

  protected ResourceExtractor(IFileSystem fileSystem) {
    handlers.put("file", this::getFileResource);
    handlers.put("jar", this::getJarResource);
    handlers.put("http", this::getHttpResource);

    _fileSystem = fileSystem;
  }

  public IFileSystem getFileSystem() {
    return _fileSystem;
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
    var jarFileDirectory = _fileSystem.createDir(dir);

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
