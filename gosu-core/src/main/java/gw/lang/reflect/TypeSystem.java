/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect;

import gw.config.CommonServices;
import gw.fs.IFile;
import gw.fs.IResource;
import gw.internal.gosu.parser.TypeLoaderAccess;
import gw.internal.gosu.parser.TypeSystemState;
import gw.lang.UnstableAPI;
import gw.lang.parser.*;
import gw.lang.parser.exceptions.ParseException;
import gw.lang.parser.exceptions.ParseResultsException;
import gw.lang.parser.expressions.ITypeLiteralExpression;
import gw.lang.reflect.gs.IGenericTypeVariable;
import gw.lang.reflect.gs.IGosuArrayClass;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.IGosuClassLoader;
import gw.lang.reflect.java.*;
import gw.lang.reflect.module.IExecutionEnvironment;
import gw.lang.reflect.module.IModule;
import gw.lang.reflect.module.IProject;
import gw.util.IFeatureFilter;
import gw.util.perf.InvocationCounter;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@UnstableAPI
public class TypeSystem {
  private static final Lock GLOBAL_LOCK = new ReentrantLock();
  public static InvocationCounter tyeRequestCounter = new InvocationCounter(false);
  public static InvocationCounter tyeLoadingCounter = new InvocationCounter(false);

  /**
   * Gets the intrinsic type for a given class.<p>
   * <p/>
   * <b>Note:</b> you should use this method only if you do not have an
   * Object of class <code>javaClass</code> to get the type from. If you
   * do have such an object, use {@link #getFromObject} instead.
   *
   * @param javaClass the Class to convert to an intrinsic type
   * @return the IType that corresponds to that class
   * @see #getFromObject(Object)
   */
  public static IType get(Class javaClass) {
    return TypeLoaderAccess.instance().get(javaClass);
  }

  public static IType get(Class javaClass, IModule module) {
    TypeSystem.pushModule(module);
    try {
      return TypeLoaderAccess.instance().get(javaClass);
    } finally {
      TypeSystem.popModule(module);
    }
  }

  public static IType get(IJavaClassInfo javaClassInfo) {
    return TypeLoaderAccess.instance().get(javaClassInfo);
  }

  public static IType get(IJavaClassInfo classInfo, IModule module) {
    TypeSystem.pushModule(module);
    try {
      return TypeLoaderAccess.instance().get(classInfo);
    } finally {
      TypeSystem.popModule(module);
    }
  }

  /**
   * Returns the intrinsic type for the given Object.
   *
   * @param object the object to get an IType for
   * @return the IType for the object
   * @see #get(Class)
   */
  public static IType getFromObject(Object object) {
    return TypeLoaderAccess.instance().getFromObject(object);
  }

  public static IType getFromObject(Object object, IModule module) {
    pushModule(module);
    try {
      return TypeLoaderAccess.instance().getFromObject(object);
    } finally {
      popModule(module);
    }
  }

  public static IType getByRelativeName(String relativeName) throws ClassNotFoundException {
    return TypeLoaderAccess.instance().getByRelativeName(relativeName);
  }

  /**
   * Gets an intrinsic type based on a relative name.  This could either be the name of an entity,
   * like "User", the name of a typekey, like "SystemPermission", or a class name, like
   * "java.lang.String" (relative and fully qualified class names are the same as far as this factory
   * is concerned).  Names can have [] appended to them to create arrays, and multi-dimensional arrays
   * are supported.
   *
   * @param relativeName the relative name of the type
   * @param typeUses     the map of used types to use when resolving
   * @return the corresponding IType
   * @throws ClassNotFoundException if the specified name doesn't correspond to any type
   */
  public static IType getByRelativeName(String relativeName, ITypeUsesMap typeUses) throws ClassNotFoundException {
    return TypeLoaderAccess.instance().getByRelativeName(relativeName, typeUses);
  }

  /**
   * Gets an intrinsic type based on a fully-qualified name.  This could either be the name of an entity,
   * like "entity.User", the name of a typekey, like "typekey.SystemPermission", or a class name, like
   * "java.lang.String".  Names can have [] appended to them to create arrays, and multi-dimensional arrays
   * are supported.
   *
   * @param fullyQualifiedName the fully qualified name of the type
   * @return the corresponding IType
   * @throws RuntimeException if the specified name doesn't correspond to any type
   */
  public static IType getByFullName(String fullyQualifiedName) {
    return TypeLoaderAccess.instance().getByFullName(fullyQualifiedName);
  }

  public static IType getByFullName(String fullyQualifiedName, IModule module) {
    TypeSystem.pushModule(module);
    try {
      return TypeLoaderAccess.instance().getByFullName(fullyQualifiedName);
    } finally {
      TypeSystem.popModule(module);
    }
  }

  /**
   * @deprecated call getByFullName( String, IModule )
   */
  public static IType getByFullName(String fullyQualifiedName, String moduleName) {
    IModule module = moduleName == null ? getExecutionEnvironment().getJreModule() : getExecutionEnvironment().getModule(moduleName);
    if (module == null) {
      throw new RuntimeException("Could not find module with name " + moduleName + " for " + fullyQualifiedName);
    }
    return getByFullName(fullyQualifiedName, module);
  }

  public static void pushGlobalModule() {
    TypeSystem.pushModule(getExecutionEnvironment().getGlobalModule());
  }

  public static void popGlobalModule() {
    TypeSystem.popModule(getExecutionEnvironment().getGlobalModule());
  }

  /**
   * Gets a type based on a fully-qualified name.  This could either be the name of an entity,
   * like "entity.User", the name of a typekey, like "typekey.SystemPermission", or a class name, like
   * "java.lang.String".  Names can have [] appended to them to create arrays, and multi-dimensional arrays
   * are supported.
   * <p>
   * This method behaves the same as getByFullName execept instead of throwing it returns null.
   *
   * @param fullyQualifiedName the fully qualified name of the type
   * @return the corresponding IType or null if the type does not exist
   */
  public static IType getByFullNameIfValid(String fullyQualifiedName) {
    return TypeLoaderAccess.instance().getByFullNameIfValid(fullyQualifiedName);
  }

  public static IType getByFullNameIfValidNoJava(String fullyQualifiedName) {
    return TypeLoaderAccess.instance().getByFullNameIfValidNoJava(fullyQualifiedName);
  }

  public static IType getByFullNameIfValid(String typeName, IModule module) {
    TypeSystem.pushModule(module);
    try {
      return getByFullNameIfValid(typeName);
    } finally {
      TypeSystem.popModule(module);
    }
  }

  public static void clearErrorTypes() {
    TypeLoaderAccess.instance().clearErrorTypes();
  }

  public static int getRefreshChecksum() {
    return TypeLoaderAccess.instance().getRefreshChecksum();
  }

  public static int getSingleRefreshChecksum() {
    return TypeLoaderAccess.instance().getSingleRefreshChecksum();
  }

  /**
   * Converts a String name of a type into an IType.
   *
   * @param typeString the type name to parse
   * @return the parsed type
   * @throws IllegalArgumentException if the type string doesn't correspond to any known IType
   */
  public static IType parseType(String typeString) throws IllegalArgumentException {
    return TypeLoaderAccess.instance().parseType(typeString);
  }

  public static IType parseType(String typeString, ITypeUsesMap typeUsesMap) throws IllegalArgumentException {
    return TypeLoaderAccess.instance().parseType(typeString, typeUsesMap);
  }

  public static IType parseType(String typeString, TypeVarToTypeMap actualParamByVarName) throws IllegalArgumentException {
    return TypeLoaderAccess.instance().parseType(typeString, actualParamByVarName);
  }

  public static IType parseType(String typeString, TypeVarToTypeMap actualParamByVarName, ITypeUsesMap typeUsesMap) throws IllegalArgumentException {
    return TypeLoaderAccess.instance().parseType(typeString, actualParamByVarName, typeUsesMap);
  }

  public static ITypeLiteralExpression parseTypeExpression(String typeString, TypeVarToTypeMap actualParamByVarName, ITypeUsesMap typeUsesMap) throws ParseResultsException {
    return TypeLoaderAccess.instance().parseTypeExpression(typeString, actualParamByVarName, typeUsesMap);
  }

  /**
   * Acquires the global type-system lock
   */
  public static void lock() {
    GLOBAL_LOCK.lock();
  }

  /**
   * Releases the global type-system lock
   */
  public static void unlock() {
    GLOBAL_LOCK.unlock();
  }

  public static Lock getGlobalLock() {
    return GLOBAL_LOCK;
  }

  public static IType getComponentType(IType valueType) {
    return TypeLoaderAccess.instance().getComponentType(valueType);
  }

  public static INamespaceType getNamespace(String strFqNamespace) {
    return TypeLoaderAccess.instance().getNamespace(strFqNamespace);
  }

  public static INamespaceType getNamespace(String strType, IModule module) {
    TypeSystem.pushModule(module);
    try {
      return getNamespace(strType);
    } finally {
      TypeSystem.popModule(module);
    }
  }

  /**
   * Returns all type names in the system for all type loaders.
   *
   * @return all type names in the system.
   */
  public static Set<? extends CharSequence> getAllTypeNames() {
    return TypeLoaderAccess.instance().getAllTypeNames();
  }

  public static ITypeVariableType getOrCreateTypeVariableType(String strName, IType boundingType, IType enclosingType) {
    return TypeLoaderAccess.instance().getOrCreateTypeVariableType(strName, boundingType, enclosingType);
  }

  public static IFunctionType getOrCreateFunctionType(IMethodInfo mi) {
    return TypeLoaderAccess.instance().getOrCreateFunctionType(mi);
  }

  public static IFunctionType getOrCreateFunctionType(String strFunctionName, IType retType, IType[] paramTypes) {
    return TypeLoaderAccess.instance().getOrCreateFunctionType(strFunctionName, retType, paramTypes);
  }

  public static <E extends IType> E getPureGenericType(E type) {
    while (type.isParameterizedType()) {
      //noinspection unchecked
      type = (E) type.getGenericType();
    }
    return type;
  }

  public static boolean isBeanType(IType typeSource) {
    return
            typeSource != GosuParserTypes.STRING_TYPE() &&
                    typeSource != GosuParserTypes.BOOLEAN_TYPE() &&
                    // typeSource != GosuParserTypes.DATETIME_TYPE() &&
                    typeSource != GosuParserTypes.NULL_TYPE() &&
                    typeSource != GosuParserTypes.NUMBER_TYPE() &&
                    !typeSource.isPrimitive() &&
                    !typeSource.isArray() &&
                    !(typeSource instanceof IFunctionType) &&
                    !(typeSource instanceof IConstructorType) &&
                    !(typeSource instanceof IMetaType);
  }

  public static boolean isNumericType(IType intrType) {
    return intrType != null && ((intrType.isPrimitive() &&
            intrType != JavaTypes.pBOOLEAN() &&
            intrType != JavaTypes.pVOID()) ||
            JavaTypes.NUMBER().isAssignableFrom(intrType) ||
            (JavaTypes.IDIMENSION().isAssignableFrom(intrType) && intrType.isFinal()) ||
            JavaTypes.CHARACTER().isAssignableFrom(intrType));

  }

  public static boolean isBoxedTypeFor(IType primitiveType, IType boxedType) {
    if (primitiveType != null && primitiveType.isPrimitive()) {
      if (primitiveType == JavaTypes.pBOOLEAN() && boxedType == JavaTypes.BOOLEAN()) {
        return true;
      }
      if (primitiveType == JavaTypes.pBYTE() && boxedType == JavaTypes.BYTE()) {
        return true;
      }
      if (primitiveType == JavaTypes.pCHAR() && boxedType == JavaTypes.CHARACTER()) {
        return true;
      }
      if (primitiveType == JavaTypes.pDOUBLE() && boxedType == JavaTypes.DOUBLE()) {
        return true;
      }
      if (primitiveType == JavaTypes.pFLOAT() && boxedType == JavaTypes.FLOAT()) {
        return true;
      }
      if (primitiveType == JavaTypes.pINT() && boxedType == JavaTypes.INTEGER()) {
        return true;
      }
      if (primitiveType == JavaTypes.pLONG() && boxedType == JavaTypes.LONG()) {
        return true;
      }
      if (primitiveType == JavaTypes.pSHORT() && boxedType == JavaTypes.SHORT()) {
        return true;
      }
      return primitiveType == JavaTypes.pVOID() && boxedType == JavaTypes.VOID();
    }
    return false;
  }

  public static TypeVarToTypeMap mapTypeByVarName(IType ownersType, IType declaringType) {
    return TypeLoaderAccess.instance().mapTypeByVarName(ownersType, declaringType);
  }

  public static IType getActualType(IType type, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars) {
    return TypeLoaderAccess.instance().getActualType(type, actualParamByVarName, bKeepTypeVars);
  }

  public static void inferTypeVariableTypesFromGenParamTypeAndConcreteType(IType genParamType, IType argType, TypeVarToTypeMap map, boolean bReverse) {
    if (bReverse) {
      TypeLoaderAccess.instance().inferTypeVariableTypesFromGenParamTypeAndConcreteType_Reverse(genParamType, argType, map);
    } else {
      TypeLoaderAccess.instance().inferTypeVariableTypesFromGenParamTypeAndConcreteType(genParamType, argType, map);
    }
  }

  public static IErrorType getErrorType() {
    return TypeLoaderAccess.instance().getErrorType();
  }

  public static IErrorType getErrorType(String strErrantName) {
    return TypeLoaderAccess.instance().getErrorType(strErrantName);
  }

  public static IErrorType getErrorType(ParseResultsException pe) {
    return TypeLoaderAccess.instance().getErrorType(pe);
  }

  public static IDefaultTypeLoader getDefaultTypeLoader() {
    return TypeLoaderAccess.instance().getDefaultTypeLoader();
  }

  public static IType findParameterizedType(IType type, IType rhsType) {
    return TypeLoaderAccess.instance().findParameterizedType(type, rhsType);
  }

  public static void addTypeLoaderListenerAsWeakRef(ITypeLoaderListener listener) {
    TypeLoaderAccess.instance().addTypeLoaderListenerAsWeakRef(listener);
  }

  public static Set<String> getNamespacesFromTypeNames(Set<? extends CharSequence> allTypeNames, Set<String> namespaces) {
    return TypeLoaderAccess.instance().getNamespacesFromTypeNames(allTypeNames, namespaces);
  }

  public static void pushTypeLoader(IModule module, ITypeLoader loader) {
    TypeLoaderAccess.instance().pushTypeLoader(module, loader);
  }

  public static void removeTypeLoader(Class<? extends ITypeLoader> loader) {
    TypeLoaderAccess.instance().removeTypeLoader(loader);
  }

  public static IType getKeyType() {
    return CommonServices.INSTANCE.getEntityAccess().getKeyType();
  }

  public static void pushIncludeAll() {
    TypeLoaderAccess.instance().pushIncludeAll();
  }

  public static void popIncludeAll() {
    TypeLoaderAccess.instance().popIncludeAll();
  }

  public static boolean isIncludeAll() {
    return TypeLoaderAccess.instance().isIncludeAll();
  }

  public static ITypeUsesMap getDefaultTypeUsesMap() {
    return CommonServices.INSTANCE.getEntityAccess().getDefaultTypeUses();
  }

  public static IType getCurrentCompilingType() {
    return TypeLoaderAccess.instance().getCurrentCompilingType();
  }

  public static IType getCompilingType(String strName) {
    return TypeLoaderAccess.instance().getCompilingType(strName);
  }

  public static void pushCompilingType(IType type) {
    TypeLoaderAccess.instance().pushCompilingType(type);
  }

  public static void popCompilingType() {
    TypeLoaderAccess.instance().popCompilingType();
  }

  public static String getUnqualifiedClassName(IType cls) {
    return cls == null ? "<null>" : cls.getRelativeName();
  }

  public static void pushSymTableCtx(ISymbolTable ctx) {
    TypeLoaderAccess.instance().pushSymTableCtx(ctx);
  }

  public static void popSymTableCtx() {
    TypeLoaderAccess.instance().popSymTableCtx();
  }

  public static ISymbolTable getSymTableCtx() {
    return TypeLoaderAccess.instance().getSymTableCtx();
  }

  public static <T extends ITypeLoader> T getTypeLoader(Class<? extends T> loaderClass) {
    return TypeLoaderAccess.instance().getTypeLoader(loaderClass, TypeSystem.getGlobalModule());
  }

  public static <T extends ITypeLoader> T getTypeLoader(Class<? extends T> loaderClass, IModule module) {
    return TypeLoaderAccess.instance().getTypeLoader(loaderClass, module);
  }

  public static String getNameOfParams(IType[] paramTypes, boolean bRelative, boolean bWithEnclosingType) {
    return TypeLoaderAccess.instance().getNameOfParams(paramTypes, bRelative, bWithEnclosingType);
  }

  public static ISymbolTable getCompiledGosuClassSymbolTable() {
    return TypeLoaderAccess.instance().getCompiledGosuClassSymbolTable();
  }

  public static List<ITypeLoader> getAllTypeLoaders() {
    return TypeLoaderAccess.instance().getAllTypeLoaders();
  }

  public static String getGenericRelativeName(IType type, boolean bRelativeBounds) {
    return getGenericName(type, true, bRelativeBounds);
  }

  public static String getGenericName(IType type) {
    return getGenericName(type, false, false);
  }

  public static String getGenericName(IType type, boolean bRelative, boolean bRelativeBounds) {
    if (!type.isGenericType() || type.isParameterizedType()) {
      return bRelative ? type.getRelativeName() : type.getName();
    }

    StringBuilder sb = new StringBuilder((bRelative ? type.getRelativeName() : type.getName()) + "<");
    IGenericTypeVariable[] typeVars = type.getGenericTypeVariables();
    for (int i = 0; i < typeVars.length; i++) {
      IGenericTypeVariable typeVar = typeVars[i];
      sb.append(typeVar.getNameWithBounds(bRelativeBounds));
      if (i < typeVars.length - 1) {
        sb.append(',');
      }
    }
    sb.append('>');
    return sb.toString();
  }

  public static IPropertyInfo getPropertyInfo(IType classBean, String strProperty, IFeatureFilter filter, IParserPart parserBase, IScriptabilityModifier scriptabilityConstraint) throws ParseException {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getPropertyInfo(classBean, strProperty, filter, parserBase, scriptabilityConstraint);
  }

  public static List<? extends IPropertyInfo> getProperties(ITypeInfo beanInfo, IType classSource) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getProperties(beanInfo, classSource);
  }

  public static List<? extends IMethodInfo> getMethods(ITypeInfo beanInfo, IType ownersIntrinsicType) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getMethods(beanInfo, ownersIntrinsicType);
  }

  @Deprecated // calls TypeSystem.get( javaClass )
  public static IType getJavaType(Class javaClass) {
    return TypeLoaderAccess.instance().getJavaType(javaClass);
  }

  public static String getNameWithQualifiedTypeVariables(IType type) {
    return TypeLoaderAccess.instance().getNameWithQualifiedTypeVariables(type);
  }

  public static IType getDefaultParameterizedType(IType type) {
    return TypeLoaderAccess.instance().getDefaultParameterizedType(type);
  }

  public static IType getDefaultParameterizedTypeWithTypeVars(IType type) {
    return TypeLoaderAccess.instance().getDefaultParameterizedTypeWithTypeVars(type);
  }

  public static boolean canCast(IType lhsType, IType rhsType) {
    return TypeLoaderAccess.instance().canCast(lhsType, rhsType);
  }

  public static void removeTypeLoaderListener(ITypeLoaderListener listener) {
    TypeLoaderAccess.instance().removeTypeLoaderListener(listener);
  }

  public static IJavaType getPrimitiveType(String name) {
    return TypeLoaderAccess.instance().getPrimitiveType(name);
  }

  public static IType getPrimitiveType(IType boxType) {
    return TypeLoaderAccess.instance().getPrimitiveType(boxType);
  }

  public static IType getBoxType(IType primitiveType) {
    return TypeLoaderAccess.instance().getBoxType(primitiveType);
  }

  public static IType[] boxPrimitiveTypeParams(IType[] typeParams) {
    IType[] newTypes = new IType[typeParams.length];
    for (int i = 0; i < typeParams.length; i++) {
      if (typeParams[i].isPrimitive()) {
        newTypes[i] = TypeSystem.getBoxType(typeParams[i]);
      } else {
        newTypes[i] = typeParams[i];
      }
    }
    return newTypes;
  }

  public static IExecutionEnvironment getExecutionEnvironment() {
    return TypeLoaderAccess.instance().getExecutionEnvironment();
  }

  public static IExecutionEnvironment getExecutionEnvironment(IProject project) {
    return TypeLoaderAccess.instance().getExecutionEnvironment(project);
  }

  public static IModule getCurrentModule() {
    return TypeLoaderAccess.instance().getCurrentModule();
  }

  /**
   * IMPORTANT: The only time you should call this method is:
   * 1) within a class implementing IType, or
   * 2) wrapping a call to a Type constructor, typically within a type loader
   * e.g., TypeSystem.getOrCreateTypeReference( new MyVeryOwnType() )
   * <p>
   * Gets or creates a type ref for the specified type.
   *
   * @param type A raw or proxied type.
   * @return If the type is already a reference, returns the type as-is, otherwise creates and returns a new type ref.
   */
  public static ITypeRef getOrCreateTypeReference(IType type) {
    return TypeLoaderAccess.instance().getOrCreateTypeReference(type);
  }

  /**
   * IMPORTANT: The only time you should call this method is:
   * 1) wrapping a call to a Type constructor, typically within a type loader
   * e.g., TypeSystem.getOrCreateTypeReference( new MyVeryOwnType() )
   * <p>
   * Do NOT call this when creating the type.  Instead call getOrCreateTypeReference
   * Gets or creates a type ref for the specified type.
   * <p>
   * This method will NOT update the type reference in the proxy.
   *
   * @param type A raw or proxied type.
   * @return returns the already created type reference or throws if the ref does not exist
   */
  public static ITypeRef getTypeReference(IType type) {
    return TypeLoaderAccess.instance().getTypeReference(type);
  }

  public static IType getTypeFromObject(Object obj) {
    return TypeLoaderAccess.instance().getTypeFromObject(obj);
  }

  /**
   * Parses a type name such as Iterable&lt;Claim&gt;.
   *
   * @param typeName the name to parse
   * @return the type
   */
  public static IType parseTypeLiteral(String typeName) {
    try {
      IType type = GosuParserFactory.createParser(typeName).parseTypeLiteral(null).getType().getType();
      if (type instanceof IErrorType) {
        throw new RuntimeException("Type not found: " + typeName);
      }
      return type;
    } catch (ParseResultsException e) {
      throw new RuntimeException("Type not found: " + typeName, e);
    }
  }

  public static boolean isExpandable(IType type) {
    return TypeLoaderAccess.instance().isExpandable(type);
  }

  public static IType boundTypes(IType targetType, List<IType> typesToBound) {
    return TypeLoaderAccess.instance().boundTypes(targetType, typesToBound);
  }

  public static IJavaClassInfo getJavaClassInfo(Class jClass) {
    return getJavaClassInfo(jClass, TypeSystem.getCurrentModule());
  }

  public static IJavaClassInfo getJavaClassInfo(Class jClass, IModule module) {
    if (jClass == null) {
      return null;
    }
    String fqn = jClass.getName().replace('$', '.');
    if (IType.class.isAssignableFrom(jClass) && fqn.endsWith(ITypeRefFactory.SYSTEM_PROXY_SUFFIX)) {
      IJavaType type = (IJavaType) get(jClass);
      return type.getBackingClassInfo();
    } else if (jClass.isArray()) {
      Class componentType = jClass.getComponentType();
      IJavaClassInfo javaClassInfo = getJavaClassInfo(componentType, module);
      return javaClassInfo.getArrayType();
    } else if (Proxy.class.isAssignableFrom(jClass)) {
      IDefaultTypeLoader defaultTypeLoader = module.getModuleTypeLoader().getDefaultTypeLoader();
      return defaultTypeLoader.getJavaClassInfoForClassDirectly(jClass, module);
    } else {
      return getJavaClassInfo(fqn, module);
    }
  }

  public static Method[] getDeclaredMethods(Class cls) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getDeclaredMethods(cls);
  }

  public static boolean isBytecodeType(IType type) {
    return type instanceof IJavaType ||
            type instanceof IGosuClass ||
            type instanceof IGosuArrayClass ||
            type instanceof IJavaArrayType ||
            type instanceof ICompoundType && ((ICompoundType) type).getTypes().stream().allMatch(TypeSystem::isBytecodeType);
  }

  public static IType getTypeFromJavaBackedType(IType type) {
    if (type instanceof IJavaType) {
      return ((IJavaType) type).getTypeFromJavaBackedType();
    } else {
      return type;
    }
  }

  public static IType getTypeFromJavaBasedType(IJavaBackedType javaType) {
    int iDims = 0;
    IJavaClassInfo ci = javaType.getBackingClassInfo();
    while (ci.isArray()) {
      iDims++;
      ci = ci.getComponentType();
    }
    IType reresovledType = TypeSystem.getByFullName(ci.getName().replace('$', '.'), javaType.getTypeLoader().getModule());
    for (int i = 0; i < iDims; i++) {
      reresovledType = reresovledType.getArrayType();
    }
    if (javaType.isParameterizedType()) {
      reresovledType = reresovledType.getParameterizedType(javaType.getTypeParameters());
    }
    return reresovledType;
  }

  public static IJavaClassInfo getJavaClassInfo(String fullyQualifiedName, IModule module) {
    if (module == null) {
      module = TypeSystem.getGlobalModule();
    }
    for (IModule m : module.getModuleTraversalList()) {
      TypeSystem.pushModule(m);
      try {
        IDefaultTypeLoader defaultTypeLoader = m.getModuleTypeLoader().getDefaultTypeLoader();
        if (defaultTypeLoader != null) {
          IJavaClassInfo javaClassInfo = defaultTypeLoader.getJavaClassInfo(fullyQualifiedName);
          if (javaClassInfo != null) {
            return javaClassInfo;
          }
        }
      } finally {
        TypeSystem.popModule(m);
      }
    }
    return null;
  }

  public static IModule getModuleFromType(IType type) {
    IModule result = null;
    if (type != null) {
      ITypeLoader loader = type.getTypeLoader();
      if (loader == null) {
        IType candidate = type.getEnclosingType();
        if (candidate != type) {
          result = getModuleFromType(candidate);
        }
        // FIXME circular loop where type == candiate implies null result.
      } else {
        result = loader.getModule();
      }
    }
    return result;
  }

  public static void pushModule(IModule gosuModule) {
    TypeLoaderAccess.instance().pushModule(gosuModule);
  }

  public static void popModule(IModule gosuModule) {
    TypeLoaderAccess.instance().popModule(gosuModule);
  }

  public static IGosuClassLoader getGosuClassLoader() {
    return TypeLoaderAccess.instance().getGosuClassLoader();
  }

  public static void dumpGosuClassLoader() {
    TypeLoaderAccess.instance().dumpGosuClassLoader();
  }

  public static IModule getGlobalModule() {
    return getExecutionEnvironment().getGlobalModule();
  }

  public static IMetaType getDefaultType() {
    return TypeLoaderAccess.instance().getDefaultType();
  }

  public static void shutdown(IExecutionEnvironment execEnv) {
    TypeSystem.pushModule(execEnv.getGlobalModule());
    TypeLoaderAccess.instance().shutdown();
  }

  public static void addShutdownListener(TypeSystemShutdownListener listener) {
    TypeLoaderAccess.instance().addShutdownListener(listener);
  }

  public static TypeSystemState getState() {
    return getExecutionEnvironment().getState();
  }

  public static String[] getTypesForFile(IModule module, IFile file) {
    return TypeLoaderAccess.instance().getTypesForFile(module, file);
  }

  public static void refresh(boolean bRefreshCaches) {
    TypeLoaderAccess.instance().refresh(bRefreshCaches);
  }

  public static void refresh(IModule module) {
    TypeLoaderAccess.instance().refresh(module);
  }

  /**
   * Refresh just the specified type i.e., a gosu editor calls this on changes
   */
  public static void refresh(ITypeRef typeRef) {
    TypeLoaderAccess.instance().refresh(typeRef);
  }

  /**
   * DO NOT USE OR DELETE. Called form the debugging process (IDE).
   *
   * @param filePaths
   */
  public static void refreshedFiles(String[] filePaths) {
    for (String filePath : filePaths) {
      IFile file = CommonServices.INSTANCE.getFileSystem().getFile(new File(filePath));
      if (file != null) {
        TypeSystem.refreshed(file);
      }
    }
  }

  public static void refreshed(IResource resource) {
    TypeLoaderAccess.instance().refreshed(resource, null, RefreshKind.MODIFICATION);
  }

  public static void deleted(IResource resource) {
    TypeLoaderAccess.instance().refreshed(resource, null, RefreshKind.DELETION);
  }

  public static void deleted(IResource resource, String typeName) {
    TypeLoaderAccess.instance().refreshed(resource, typeName, RefreshKind.DELETION);
  }

  public static void created(IResource resource) {
    TypeLoaderAccess.instance().refreshed(resource, null, RefreshKind.CREATION);
  }

  public static void created(IResource resource, String typeName) {
    TypeLoaderAccess.instance().refreshed(resource, typeName, RefreshKind.CREATION);
  }

  public static boolean isDeleted(IType type) {
    return type instanceof ITypeRef && // a type that's not proxied is never deleted
            ((ITypeRef) type).isDeleted();
  }

  public static IType replaceTypeVariableTypeParametersWithBoundingTypes(IType type, IType enclosingType) {
    return TypeLoaderAccess.instance().replaceTypeVariableTypeParametersWithBoundingTypes(type, enclosingType);
  }

  public static boolean isParameterizedWith(IType type, ITypeVariableType... typeVar) {
    return TypeLoaderAccess.instance().isParameterizedWith(type, typeVar);
  }

  public static IModule getJreModule() {
    return getExecutionEnvironment().getJreModule();
  }

  public static IType getCompoundType(Set<IType> types) {
    return TypeLoaderAccess.instance().getCompoundType(types);
  }

  public static IType getFunctionalInterface(IFunctionType type) {
    return TypeLoaderAccess.instance().getFunctionalInterface(type);
  }
}

