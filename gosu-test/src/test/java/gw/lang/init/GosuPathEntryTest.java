/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.init;

import gw.fs.IDirectory;
import gw.internal.gosu.module.fs.FileSystemImpl;
import gw.test.TestClass;
import gw.internal.gosu.module.fs.resource.JavaDirectoryImpl;
import gw.lang.reflect.module.IFileSystem;

import java.util.Collections;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: akeefer
 * Date: May 17, 2010
 * Time: 3:14:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class GosuPathEntryTest extends TestClass {

  public void testConstructorThrowsIllegalArgumentExceptionIfRootIsNull() {
    try {
      new GosuPathEntry(null, Collections.<IDirectory>emptyList());
      fail();
    } catch (IllegalArgumentException e) {
      // Expected();
    }

  }

  public void testConstructorThrowsIllegalArgumentExceptionIfSrcsIsNull() {
    var fs = new FileSystemImpl(IFileSystem.CachingMode.NO_CACHING);

    try {
      new GosuPathEntry(new JavaDirectoryImpl(fs, new File("foo/bar"), IFileSystem.CachingMode.NO_CACHING), null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected();
    }
  }

  public void testConstructorDoesNothingElseIfArgumentsAreValid() {
    var fs = new FileSystemImpl(IFileSystem.CachingMode.NO_CACHING);

    new GosuPathEntry(new JavaDirectoryImpl(fs, new File("foo/bar"), IFileSystem.CachingMode.NO_CACHING), Collections.<IDirectory>emptyList());
  }
}
