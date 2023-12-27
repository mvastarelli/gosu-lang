/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang;

import gw.config.CommonServices;
import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.lang.gosuc.ICustomParser;
import gw.lang.gosuc.IGosuc;
import gw.lang.init.GosuPathEntry;
import gw.lang.ir.IRClassCompiler;
import gw.lang.ir.IRTypeResolver;
import gw.lang.javadoc.IJavaDocFactory;
import gw.lang.parser.IConstructorInfoFactory;
import gw.lang.parser.IDynamicFunctionSymbol;
import gw.lang.parser.IExpression;
import gw.lang.parser.IFullParserState;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.IReducedDynamicFunctionSymbol;
import gw.lang.parser.IScope;
import gw.lang.parser.ISourceCodeTokenizer;
import gw.lang.parser.IStackProvider;
import gw.lang.parser.ISymbol;
import gw.lang.parser.ISymbolTable;
import gw.lang.parser.ITokenizerInstructor;
import gw.lang.parser.ITypeUsesMap;
import gw.lang.parser.expressions.IIdentifierExpression;
import gw.lang.parser.expressions.INullExpression;
import gw.lang.parser.template.ITemplateHost;
import gw.lang.parser.template.TemplateParseException;
import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IAnnotationInfoFactory;
import gw.lang.reflect.IEntityAccess;
import gw.lang.reflect.IFeatureInfo;
import gw.lang.reflect.IFunctionType;
import gw.lang.reflect.IMetaType;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IPropertyAccessor;
import gw.lang.reflect.IPropertyInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeInfo;
import gw.lang.reflect.ITypeInfoFactory;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.GosuClassTypeLoader;
import gw.lang.reflect.gs.IEnhancementIndex;
import gw.lang.reflect.gs.IFileSystemGosuClassRepository;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.IGosuEnhancement;
import gw.lang.reflect.gs.IGosuProgram;
import gw.lang.reflect.gs.ISourceFileHandle;
import gw.lang.reflect.gs.ITemplateType;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.module.IClassPath;
import gw.lang.reflect.module.IExecutionEnvironment;
import gw.lang.reflect.module.IModule;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import gw.util.Array;
import manifold.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GosuShop
{
  public static ISymbolTable createSymbolTable()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSymbolTable();
  }

  public static ISymbolTable createSymbolTable( boolean bDefineCommonSymbols )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSymbolTable( bDefineCommonSymbols );
  }

  public static ITemplateHost createTemplateHost()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createTemplateHost();
  }

  public static IPropertyInfo createLengthProperty(ITypeInfo typeInfo)
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createLengthProperty(typeInfo);
  }

  public static IFunctionType createFunctionType( IMethodInfo mi )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createFunctionType( mi );
  }

  public static ITypeInfoFactory getTypeInfoFactory()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getTypeInfoFactory();
  }

  public static IConstructorInfoFactory getConstructorInfoFactory()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getConstructorInfoFactory();
  }

  public static IAnnotationInfoFactory getAnnotationInfoFactory()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getAnnotationInfoFactory();
  }

  public static IJavaDocFactory getJavaDocFactory()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getJavaDocFactory();
  }

  public static ISymbol createSymbol( CharSequence name, IType type, Object value )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSymbol( name, type, value );
  }
  public static ISymbol createSymbol( CharSequence name, IType type, IStackProvider stackProvider )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSymbol( name, type, stackProvider );
  }

  public static IClassPath createClassPath(IModule module, boolean includeAllClasses)
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createClassPath(module, includeAllClasses);
  }

  public static IEntityAccess getDefaultEntityAccess()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getDefaultEntityAccess();
  }

  public static ITemplateHost createSimpleTemplateHost()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSimpleTemplateHost();
  }

  public static ISourceCodeTokenizer createSourceCodeTokenizer( CharSequence code )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSourceCodeTokenizer( code );
  }
  public static ISourceCodeTokenizer createSourceCodeTokenizer( CharSequence code, boolean bTemplate )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSourceCodeTokenizer( code, bTemplate );
  }
  public static ISourceCodeTokenizer createSourceCodeTokenizer( Reader reader )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createSourceCodeTokenizer( reader );
  }
  public static ITokenizerInstructor createTemplateInstructor( ISourceCodeTokenizer tokenizer )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createTemplateInstructor( tokenizer );
  }

  public static IScope createCommonSymbolScope()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createCommnoSymbolScope();
  }

  public static IIdentifierExpression createIdentifierExpression()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createIdentifierExpression();
  }

  public static void generateTemplate( Reader readerTemplate, Writer writerOut, ISymbolTable threadLocalSymbolTable ) throws TemplateParseException
  {
    CommonServices.INSTANCE.getGosuIndustrialPark().generateTemplate( readerTemplate, writerOut, threadLocalSymbolTable );
  }

  public static ITokenizerInstructor createTemplateTokenizerInstructor( ISourceCodeTokenizer tokenizer )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createTemplateTokenizerInstructor( tokenizer );
  }

  public static ISymbolTable getGosuClassSymbolTable()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getGosuClassSymbolTable();
  }

  public static ISymbol createDynamicFunctionSymbol( ISymbolTable symbolTable, String strMemberName, IFunctionType functionType, List<ISymbol> params, IExpression expression )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createDynamicFunctionSymbol( symbolTable, strMemberName, functionType, params, expression );
  }

  public static IEnhancementIndex createEnhancementIndex( GosuClassTypeLoader loader )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createEnhancementIndex( loader );
  }

  public static IGosuClass createClass( String strNamespace, String strRelativeName, GosuClassTypeLoader loader, ISourceFileHandle sourceFile, ITypeUsesMap typeUsesMap )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createClass( strNamespace, strRelativeName, loader, sourceFile, typeUsesMap );
  }
  public static IGosuProgram createProgram( String strNamespace, String strRelativeName, GosuClassTypeLoader loader, ISourceFileHandle sourceFile, ITypeUsesMap typeUsesMap, ISymbolTable symTable )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createProgram( strNamespace, strRelativeName, loader, sourceFile, typeUsesMap, symTable );
  }
  public static IGosuProgram createProgramForEval( String strNamespace, String strRelativeName, GosuClassTypeLoader loader, ISourceFileHandle sourceFile, ITypeUsesMap typeUsesMap, ISymbolTable symTable )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createProgramForEval( strNamespace, strRelativeName, loader, sourceFile, typeUsesMap, symTable );
  }
  public static IGosuEnhancement createEnhancement( String strNamespace, String strRelativeName, GosuClassTypeLoader loader, ISourceFileHandle sourceFile, ITypeUsesMap typeUsesMap )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createEnhancement( strNamespace, strRelativeName, loader, sourceFile, typeUsesMap );
  }
  public static ITemplateType createTemplate( String strNamespace, String strRelativeName, GosuClassTypeLoader loader, ISourceFileHandle sourceFile, ITypeUsesMap typeUsesMap, ISymbolTable symTable )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createTemplate( strNamespace, strRelativeName, loader, sourceFile, typeUsesMap, symTable );
  }

  public static IFileSystemGosuClassRepository createFileSystemGosuClassRepository(IModule module, IDirectory[] files)
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createFileSystemGosuClassRepository(module, files);
  }
  public static IFileSystemGosuClassRepository createFileSystemGosuClassRepository(IModule module, IDirectory[] files, String[] extensions)
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createFileSystemGosuClassRepository(module, files, extensions);
  }

  public static ITypeUsesMap createTypeUsesMap( List<String> specialTypeUses )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createTypeUsesMap(specialTypeUses);
  }

  public static IFullParserState createStandardParserState( IParsedElement rootParsedElement, String scriptSrc, boolean b )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createStandardParserState(rootParsedElement, scriptSrc, b);
  }

  public static RuntimeException createEvaluationException(String msg) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createEvaluationException(msg);
  }

  public static IPropertyInfo createPropertyDelegate(IFeatureInfo container, IPropertyInfo prop) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createPropertyDelegate(container, prop);
  }

  public static IModule createModule( IExecutionEnvironment execEnv, String strMemberName )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createModule( execEnv, strMemberName );
  }

  public static IModule createGlobalModule(IExecutionEnvironment execEnv)
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createGlobalModule(execEnv);
  }

  public static INullExpression getNullExpressionInstance() {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getNullExpressionInstance();
  }

  public static IGosuClass getBlockToInterfaceConversionClass( IType typeToCoerceTo, IType enclosingType ) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getBlockToInterfaceConversionClass( typeToCoerceTo, enclosingType );
  }

  public static IRTypeResolver getIRTypeResolver() {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getIRTypeResolver();
  }

  public static IRClassCompiler getIRClassCompiler() {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getIRClassCompiler();
  }

  public static IPropertyAccessor getLengthAccessor()
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getLengthAccessor();
  }

  public static GosuPathEntry createPathEntryFromModuleFile(IFile f) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createPathEntryFromModuleFile(f);
  }

  static Set<String> SPECIAL_PUBLISH_TYPES = new HashSet<String>() {{
    this.add("gw.internal.xml.ws.IWsdlConfig");
    this.add("gw.internal.xml.ws.rt.WsdlPortImpl");
    this.add("gw.xml.ws.IWsdlPort");
    this.add("gw.xml.ws.Wsdl2Gosu");
    this.add("gw.xml.XmlElement");
  }};

  public static Map<IType, IType> getPublishedTypeMap(IJavaClassInfo classInfo) {
    if (SPECIAL_PUBLISH_TYPES.contains(classInfo.getName())) {
      IAnnotationInfo publishedTypesAnnotation = classInfo.getAnnotation(PublishedTypes.class);
      if (publishedTypesAnnotation != null) {
        Map<IType, IType> map = new HashMap<IType, IType>();
        Object value = publishedTypesAnnotation.getFieldValue("value");
        IAnnotationInfo[] publishedTypes;
        if (value.getClass().isArray()) {
          publishedTypes = (IAnnotationInfo[]) value;
        } else {
          publishedTypes = new IAnnotationInfo[]{(IAnnotationInfo) value};
        }
        for (IAnnotationInfo publishedType : publishedTypes) {
          IType fromType = TypeSystem.parseTypeLiteral((String) publishedType.getFieldValue("fromType"));
          IType toType = TypeSystem.parseTypeLiteral((String) publishedType.getFieldValue("toType"));
          map.put(fromType, toType);
        }
        return map;
      }
    }
    return null;
  }

  public static IReducedDynamicFunctionSymbol createReducedDynamicFunctionSymbol(IDynamicFunctionSymbol symbol) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createReducedDynamicFunctionSymbol(symbol);
  }

  public static List<String> urls2paths(List<URL> urls) {
    List<String> paths = new ArrayList<String>( urls.size() );
    for( URL url : urls )
    {
      String path = URLDecoder.decode(new File(url.getFile()).getAbsolutePath());
      paths.add( path );
    }
    return paths;
  }

  public static void clearThreadLocal(ThreadLocal tl) {
    ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
    while (root.getParent() != null) {
      root = root.getParent();
    }
    visit(root, tl, 0);
  }

  private static void visit(ThreadGroup group, ThreadLocal tl, int level) {
    // Get threads in `group'
    int numThreads = group.activeCount();
    Thread[] threads = new Thread[numThreads * 2];
    numThreads = group.enumerate(threads, false);

    // Enumerate each thread in `group'
    for (int i = 0; i < numThreads; i++) {
      Thread thread = threads[i];
      clearThreadLocal(tl, thread);
    }

    // Get thread subgroups of `group'
    int numGroups = group.activeGroupCount();
    ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
    numGroups = group.enumerate(groups, false);

    // Recursively visit each subgroup
    for (int i = 0; i < numGroups; i++) {
      visit(groups[i], tl, level + 1);
    }
  }

  private static void clearThreadLocal(ThreadLocal tl, Thread thread) {
    Object map = getMap(thread);
    if (map != null) {
      ReflectUtil.method( map.getClass(), "set", ThreadLocal.class, Object.class ).invoke(map, tl, null);
    }
  }

  private static Object getMap(Thread thread) {
    return ReflectUtil.field( Thread.class, "threadLocals" ).get(thread);
  }

  public static boolean isGosuFile(String fileName) {
    int i = fileName.lastIndexOf('.');
    if (i >= 0) {
      return GosuClassTypeLoader.ALL_EXTS_SET.contains(fileName.substring(i));
    }
    return false;
  }

  public static IGosuc makeGosucCompiler( String gosucProjectFile, ICustomParser custParser ) {
    return CommonServices.getTypeSystem().makeGosucCompiler( gosucProjectFile, custParser );
  }

  public static IModule getModule(IType type) {
    IModule module = null;
    while (module == null && type != null) {
      if (type.getTypeLoader() == null) {
        type = type.getEnclosingType();
      } else {
        module = type.getTypeLoader().getModule();
      }
    }
    return module;
  }

  public static IType getPureGenericType(IType type) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getPureGenericType(type);
  }

  public static Object getAnnotationFieldValueAsArray(IAnnotationInfo annotationInfo, String field) {
    Object value = annotationInfo.getFieldValue(field);
    if (value.getClass().isArray()) {
      return value;
    } else {
      Object array = Array.newInstance(value.getClass(), 1);
      Array.set(array, 0, value);
      return array;
    }
  }

  public static IJavaClassInfo createClassInfo(Class aClass, IModule module) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createClassInfo(aClass, module);
  }

  public static String toSignature(String fullyQualifiedName) {
    if (fullyQualifiedName.equals(byte.class.getName())) {
      return "B";
    } else if (fullyQualifiedName.equals(char.class.getName())) {
      return "C";
    } else if (fullyQualifiedName.equals(double.class.getName())) {
      return "D";
    } else if (fullyQualifiedName.equals(float.class.getName())) {
      return "F";
    } else if (fullyQualifiedName.equals(int.class.getName())) {
      return "I";
    } else if (fullyQualifiedName.equals(long.class.getName())) {
      return "J";
    } else if (fullyQualifiedName.equals(short.class.getName())) {
      return "S";
    } else if (fullyQualifiedName.equals(boolean.class.getName())) {
      return "Z";
    } else if (fullyQualifiedName.equals(void.class.getName())) {
      return "V";
    } else if (fullyQualifiedName.endsWith("[]")) {
      return '[' + toSignature(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2));
    } else {
      return 'L' + fullyQualifiedName + ';';
    }
  }

  public static boolean contains(IType[] types, IType type) {
    for (IType iType : types) {
      if (iType.equals(type)) {
        return true;
      }
    }
    return false;
  }

  public static IMetaType createMetaType(IType type, boolean literal) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().createMetaType(type, literal);
  }

  public static byte[] updateReloadClassesIndicator(List<String> changedTypes, String strScript ) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().updateReloadClassesIndicator(changedTypes, strScript);
  }

  public static void print( Object ret ) {
    CommonServices.INSTANCE.getGosuIndustrialPark().print( ret );
  }

  public static String toString( Object val ) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().toString( val );
  }

  public static IGosuClass getGosuClassFrom( IType fromType ) {
    return CommonServices.INSTANCE.getGosuIndustrialPark().getGosuClassFrom( fromType );
  }

  public static String generateJavaStub( IGosuClass gsClass )
  {
    return CommonServices.INSTANCE.getGosuIndustrialPark().generateJavaStub( gsClass );
  }
}
