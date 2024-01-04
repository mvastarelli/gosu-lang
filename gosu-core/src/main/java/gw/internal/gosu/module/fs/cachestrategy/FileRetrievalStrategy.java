package gw.internal.gosu.module.fs.cachestrategy;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FileRetrievalStrategy {
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

  private final IFileSystem _fileSystem;
  private final JavaDirectoryImpl _parent;

  protected FileRetrievalStrategy(IFileSystem fileSystem, JavaDirectoryImpl parent) {
    _fileSystem = fileSystem;
    _parent = parent;
  }

  protected  IFileSystem getFileSystem() {
    return _fileSystem;
  }

  protected JavaDirectoryImpl getParent() {
    return _parent;
  }

  public abstract List<? extends IDirectory> listDirs();

  public abstract List<? extends IFile> listFiles();

  public abstract boolean hasChildFile(String path);

  protected static boolean isDirectory(File f) {
    String name = f.getName();

    if (isAssumedFileSuffix(getFileSuffix(name))) {
      return false;
    } else {
      return f.isDirectory();
    }
  }

  protected static String getFileSuffix(String name) {
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex == -1) {
      return null;
    } else {
      return name.substring(dotIndex + 1);
    }
  }

  protected static boolean isAssumedFileSuffix(String suffix) {
    return FILE_SUFFIXES.contains(suffix);
  }
}
