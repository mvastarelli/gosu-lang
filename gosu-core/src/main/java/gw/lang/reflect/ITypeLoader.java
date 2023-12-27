/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect;

import gw.config.IService;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.lang.reflect.gs.TypeName;
import gw.lang.reflect.module.IModule;

import java.net.URL;
import java.util.List;
import java.util.Set;

public interface ITypeLoader extends IService
{
  public static final String[] NO_TYPES = new String[0];

  /**
   * @return The module to which this type loader belongs.
   */
  IModule getModule();

  /**
   * Gets a type based on a fully-qualified name.  This could either be the name of an entity,
   * like "entity.User", the name of a typekey, like "typekey.SystemPermission", or a class name, like
   * "java.lang.String".  Names can have [] appended to them to create arrays, and multi-dimensional arrays
   * are supported.<p>
   * <p/>
   * If the type can be successfully resolved by the typeloader, it will be returned, otherwise it will
   * return null.  The sole exception to this rule is the top-level TypeLoaderAccess, which will throw
   * a {@link ClassNotFoundException} if none of its composite typeloaders can load the type.<p>
   * <p/>
   * <p/>
   * There is a global lock in TypeLoaderAccess that is acquired when this method is called.  Basically
   * one type at a time can be loaded from the system.  This method is free to release that lock during this call.
   * This is needed to deal with reentrant type loaders.  It is the responsibility of this method to make sure the
   * lock is reacquired before this method returns.
   * <p/>
   * Type loader access will guarentee that no duplicate types are put into the type loader.
   *
   * @param fullyQualifiedName the fully qualified name of the type
   *
   * @return the corresponding IType or null
   */
  IType getType( String fullyQualifiedName );

  /**
   * @return the set of fully qualified type names this loader is responsible for
   *         loading. Note due to the dynamic nature of some type loaders, there is no
   *         guarantee that all types for a given loader are known at the time this
   *         method is called.
   */
  Set<? extends CharSequence> getAllTypeNames();

  public boolean showTypeNamesInIDE();

  /**
   * Don't call this method unless you really know what you're doing.
   *
   * @return the set of package (aka namespace) names in which this loader's
   *         types reside.
   */
  Set<? extends CharSequence> getAllNamespaces();

  /**
   * Finds the resource with the given name.  A resource is some data
   * that can be accessed by class code in a way that may be independent
   * of the location of the code.  The exact location of the resource is
   * dependent upon the loader implementation
   * <p/>
   * <p> The name of a resource is a '<tt>/</tt>'-separated path name that
   * identifies the resource.
   *
   * @param name The resource name
   *
   * @return A <tt>URL</tt> object for reading the resource, or
   *         <tt>null</tt> if the resource could not be found or the invoker
   *         doesn't have adequate  privileges to get the resource.
   */
  URL getResource( String name );

  boolean isCaseSensitive();

  List<String> getHandledPrefixes();

  boolean handlesNonPrefixLoads();
  
  boolean handlesFile(IFile file);

  /**
   * Returns ALL type names associated with the given file
   * whether or not the types have been loaded yet.
   * Type loading should NOT be used in the implementation of this method.
   *
   * @param file The file in question
   * @return All known types derived from that file
   */
  String[] getTypesForFile(IFile file);

  /**
   * Notifies the type loader that a file has been refreshed.  The type loader should return all
   * types that it knows need to be refreshed based on the given file.

   * @param file The file that was refreshed
   * @param types
   * @param kind  @return All known types affected by the file change
   */
  RefreshKind refreshedFile(IFile file, String[] types, RefreshKind kind);

  void refreshedNamespace(String namespace, IDirectory dir, RefreshKind kind);

  /**
   * Fired when an existing type is refreshed, i.e. there are potential changes
   * @param request
   */
  public void refreshedTypes(RefreshRequest request);

  /**
   * Fired when the typesystem is fully refreshed
   */
  public void refreshed();

  boolean handlesDirectory(IDirectory dir);

  String getNamespaceForDirectory(IDirectory dir);

  boolean hasNamespace(String namespace);

  Set<TypeName> getTypeNames(String namespace);

  Set<String> computeTypeNames();

  void shutdown();
}
