/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect.gs;

import gw.lang.init.GosuRuntimeManifoldHost;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeLoader;
import manifold.api.host.IModule;

//## todo: delete this class and use manifold's TypeName
public class TypeName extends manifold.api.type.TypeName {
  public final String name;
  public final Kind kind;
  public final Visibility visibility;
  public final ITypeLoader loader;

  public TypeName(String name, ITypeLoader loader, Kind kind, Visibility visibility) {
    super(name, GosuRuntimeManifoldHost.get().getSingleModule(), kind, visibility );
    this.name = name;
    this.loader = loader;
    this.kind = kind;
    this.visibility = visibility;
  }

  public TypeName(IType innerType) {
    this(innerType.getName(), innerType.getTypeLoader(), Kind.TYPE, Visibility.PUBLIC);
  }

  @Override
  public int compareTo(Object o) {
    return -(kind.ordinal() - ((TypeName)o).kind.ordinal());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TypeName typeName = (TypeName) o;

    if (kind != typeName.kind) return false;
    if (loader != null ? !loader.equals(typeName.loader) : typeName.loader != null) return false;
    if (name != null ? !name.equals(typeName.name) : typeName.name != null) return false;
    if (visibility != typeName.visibility) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    result = 31 * result + (visibility != null ? visibility.hashCode() : 0);
    result = 31 * result + (loader != null ? loader.hashCode() : 0);
    return result;
  }

  public IModule getModule() {
    return GosuRuntimeManifoldHost.get().getSingleModule();
  }
  public gw.lang.reflect.module.IModule getGosuModule() {
    return loader.getModule();
  }

  @Override
  public String toString() {
    return kind + " " + name + ": " + visibility;
  }
}
