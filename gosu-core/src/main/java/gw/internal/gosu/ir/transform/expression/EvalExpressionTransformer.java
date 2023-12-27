/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.transform.expression;

import gw.config.CommonServices;
import gw.internal.gosu.compiler.SingleServingGosuClassLoader;
import gw.internal.gosu.ir.transform.ExpressionTransformer;
import gw.internal.gosu.ir.transform.TopLevelTransformationContext;
import gw.internal.gosu.parser.IGosuProgramInternal;
import gw.internal.gosu.parser.expressions.EvalExpression;
import gw.internal.gosu.util.LRUMap;
import gw.lang.ir.IRExpression;
import gw.lang.parser.GosuParserFactory;
import gw.lang.parser.ICapturedSymbol;
import gw.lang.parser.IGosuProgramParser;
import gw.lang.parser.IParseResult;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.ISymbolTable;
import gw.lang.reflect.IType;
import gw.lang.reflect.LazyTypeResolver;
import gw.lang.reflect.gs.ICompilableType;
import gw.lang.reflect.gs.IExternalSymbolMap;
import gw.lang.reflect.gs.IGosuProgram;
import gw.lang.reflect.gs.IProgramInstance;
import gw.util.GosuExceptionUtil;
import gw.util.GosuStringUtil;
import gw.util.concurrent.LocklessLazyVar;
import manifold.util.ReflectUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 */
public class EvalExpressionTransformer extends EvalBasedTransformer<EvalExpression>
{
  @SuppressWarnings("unchecked")
  // ##todo: use a ConcurrentHashMap here somehow i.e., stop using Collections.synchronizedMap(), it's a perf issue here
  public static final Map<String, EvalExpression> EVAL_EXPRESSIONS = Collections.synchronizedMap( new LRUMap( 2000 ) );

  public static interface DeclaredConstructorsAccessor {
    Constructor getConstructor( Class clz );
  }

  public static LocklessLazyVar<DeclaredConstructorsAccessor> _ctorAccessor = new LocklessLazyVar<DeclaredConstructorsAccessor>()
  {
    @Override
    protected DeclaredConstructorsAccessor init()
    {
      Method result = null;
      try
      {
        result = ReflectUtil.method( Class.class, "privateGetDeclaredConstructors", boolean.class ).getMethod();
      }
      catch( Exception ignore )
      {
      }
      if( result != null )
      {
        return new PrivateGetDeclaredConstructorsAccessor( result );
      }
      else
      {
        return new PublicGetDeclaredConstructorsAccessor();
      }
    }
  };

  private static class PrivateGetDeclaredConstructorsAccessor implements DeclaredConstructorsAccessor
  {
    private Method _getDeclaredConstructors;
    public PrivateGetDeclaredConstructorsAccessor( Method getDeclaredConstructors )
    {
      _getDeclaredConstructors = getDeclaredConstructors;
    }

    @Override
    public Constructor getConstructor( final Class clz )
    {
      try
      {
        return ((Constructor[])_getDeclaredConstructors.invoke( clz, false ))[0];
      }
      catch( Exception e )
      {
        System.err.println( "WARNING Cannot load constructors of " + clz.getName() + ": " + e.toString() );
        e.printStackTrace();
        return null;
      }
    }
  }
  private static class PublicGetDeclaredConstructorsAccessor implements DeclaredConstructorsAccessor {
    @Override
    public Constructor getConstructor( Class clz ) {
      return clz.getConstructors()[0];
    }
  }

  
  public static IRExpression compile( TopLevelTransformationContext cc, EvalExpression expr )
  {
    EvalExpressionTransformer compiler = new EvalExpressionTransformer( cc, expr );
    return compiler.compile();
  }

  private EvalExpressionTransformer( TopLevelTransformationContext cc, EvalExpression expr )
  {
    super( cc, expr );
  }

  protected IRExpression compile_impl()
  {
    putEvalExpression( _expr() );
    return callStaticMethod( EvalExpressionTransformer.class, "compileAndRunEvalSource", new Class[]{Object.class, Object.class, Object[].class, LazyTypeResolver[].class, IType.class, int.class, int.class, String.class},
            exprList(
                    boxValue( _expr().getType(), ExpressionTransformer.compile( _expr().getExpression(), _cc() ) ),
                    pushEnclosingContext(),
                    pushCapturedSymbols( getGosuClass(), _expr().getCapturedForBytecode() ),
                    pushEnclosingFunctionTypeParamsInArray( _expr() ),
                    pushType( getGosuClass() ),
                    pushConstant( _expr().getLineNum() ),
                    pushConstant( _expr().getColumn() ),
                    pushConstant( _expr().toString() )
            ));
  }

  private void putEvalExpression( EvalExpression evalExpr )
  {
    synchronized( EVAL_EXPRESSIONS )
    {
      int iLine = _expr().getLineNum();
      int iColumnNum = _expr().getColumn();
      EVAL_EXPRESSIONS.put( makeEvalKey( getGosuClass(), iLine, iColumnNum, _expr().toString() ), evalExpr );
    }
  }

  public static String makeEvalKey( IType enclosingClass, int iLineNum, int iColumnNum, String evalExprText ) {
    return enclosingClass.getName() + '.' + IGosuProgram.NAME_PREFIX + "eval_" + iLineNum + ":" + iColumnNum + ":" + GosuStringUtil.getSHA1String( evalExprText );
  }

  public static Object compileAndRunEvalSource( Object source, Object outer, Object[] capturedValues,
                                                LazyTypeResolver[] immediateFuncTypeParams, IType enclosingClass,
                                                int iLineNum, int iColumn, String evalExprText )
  {
    String evalExprKey = makeEvalKey( enclosingClass, iLineNum, iColumn, evalExprText );
    EvalExpression evalExpr = EVAL_EXPRESSIONS.get( evalExprKey );
    if( evalExpr == null && enclosingClass.isCompilable() ) {
      enclosingClass.compile(); // force compilation of enclosing class indirectly compiles eval-expr which caches the expr
      evalExpr = EVAL_EXPRESSIONS.get( evalExprKey );
    }
    return compileAndRunEvalSource( source, outer, capturedValues, immediateFuncTypeParams, enclosingClass, evalExpr );
  }

  public static Object compileAndRunEvalSource( Object source, Object outer, Object[] capturedValues,
                                                LazyTypeResolver[] immediateFuncTypeParams, IType enclosingClass, IParsedElement evalExpr )
  {
    return compileAndRunEvalSource( source, outer, capturedValues, immediateFuncTypeParams, enclosingClass, evalExpr, null, null );
  }
  public static Object compileAndRunEvalSource( Object source, Object outer, Object[] capturedValues,
                                                LazyTypeResolver[] immediateFuncTypeParams, IType enclosingClass, IParsedElement evalExpr,
                                                ISymbolTable compileTimeLocalContextSymbols, IExternalSymbolMap runtimeLocalSymbolValues )
  {
    String strSource = CommonServices.INSTANCE.getCoercionManager().makeStringFrom( source );
    IGosuProgramParser parser = GosuParserFactory.createProgramParser();
    List<ICapturedSymbol> capturedSymbols = evalExpr instanceof EvalExpression ? ((EvalExpression)evalExpr).getCapturedForBytecode() : Collections.<ICapturedSymbol>emptyList();
    //debugInfo( compileTimeLocalContextSymbols );
    IParseResult res = parser.parseEval( strSource, capturedSymbols, enclosingClass, evalExpr, compileTimeLocalContextSymbols );
    IGosuProgram gp = res.getProgram();
    if( !gp.isValid() )
    {
      System.out.println( gp.getParseResultsException() );
      throw GosuExceptionUtil.forceThrow(gp.getParseResultsException());
    }

    Class<?> javaClass = gp.getBackingClass();
    ClassLoader classLoader = javaClass.getClassLoader();
    assert classLoader instanceof SingleServingGosuClassLoader;
    List<Object> args = new ArrayList<Object>();
    if( !gp.isStatic() )
    {
      args.add( outer );
    }

    addCapturedValues( (IGosuProgramInternal)gp, capturedSymbols, capturedValues, args );

    addEnclosingTypeParams( immediateFuncTypeParams, args );

    Constructor ctor = _ctorAccessor.get().getConstructor( javaClass );
    Class[] parameterTypes = ctor.getParameterTypes();
    if( parameterTypes.length != args.size() )
    {
      if( parameterTypes.length > args.size() &&
          parameterTypes[parameterTypes.length-1].getName().equals( IExternalSymbolMap.class.getName() ) )
      {
        args.add( runtimeLocalSymbolValues );
      }
      else
      {
        throw new IllegalStateException( "Eval constructor param count is not " + args.size() + "\nPassed in args " + printArgs( args ) + "\nActual args: " + printArgs( ctor.getParameterTypes() ) );
      }
    }
    try
    {
//      Class[] parameterTypes = ctor.getParameterTypes();
//      for( int i = 0; i < parameterTypes.length; i++ ) {
//        System.out.println( "PARAM: " + parameterTypes[i].getName() + "  ARG: " + args.get( i ) );
//      }
      IProgramInstance evalInstance = (IProgramInstance)ctor.newInstance( args.toArray() );
      return evalInstance.evaluate( runtimeLocalSymbolValues );
    }
    catch( Exception e )
    {
      throw GosuExceptionUtil.forceThrow( e );
    }
  }

  private static String printArgs( List<Object> args ) {
    String str = "";
    for( Object a: args ) {
      str += a + ", ";
    }
    return str;
  }

  private static String printArgs( Class[] parameterTypes ) {
    String str = "";
    for( Class c: parameterTypes ) {
      str += c.getName() + ", ";
    }
    return str;
  }

  private static void debugInfo( ISymbolTable compileTimeLocalContextSymbols ) {
    if( compileTimeLocalContextSymbols != null ) {
      Map symbols = compileTimeLocalContextSymbols.getSymbols();
      for( Object key : symbols.keySet() ) {
        Object o = symbols.get( key );
        System.out.println( "SYMBOL NAME: " + key + " SYMBOL: " + o );
      }
    }
  }

  private static void addCapturedValues( IGosuProgramInternal gp, List<ICapturedSymbol> capturedSymbols, Object[] capturedValues, List<Object> args )
  {
    if( capturedValues != null )
    {
      // Note: must add the captured symbols in the order of the eval class' ctor, which is the order of the values in its map of captured symbols.

      Map<String, ICapturedSymbol> capturedSymbolsByName = gp.getCapturedSymbols();
      if( capturedSymbolsByName != null )
      {
        for( ICapturedSymbol sym : capturedSymbolsByName.values() )
        {
          args.add( capturedValues[capturedSymbols.indexOf( sym )] );
        }
      }

      if (requiresExternalSymbolCapture(gp)) {
        args.add( capturedValues[capturedValues.length - 1] );
      }
    }
  }

  public static void clearEvalExpressions() {
    EVAL_EXPRESSIONS.clear();
  }

}
