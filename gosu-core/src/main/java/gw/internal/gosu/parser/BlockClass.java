/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.config.CommonServices;
import gw.internal.gosu.compiler.FunctionClassUtil;
import gw.internal.gosu.parser.expressions.BeanMethodCallExpression;
import gw.internal.gosu.parser.expressions.BlockExpression;
import gw.internal.gosu.parser.expressions.Identifier;
import gw.internal.gosu.parser.statements.ExpressionStatement;
import gw.internal.gosu.parser.statements.ReturnStatement;
import gw.lang.reflect.java.JavaTypes;
import gw.lang.function.IBlock;
import gw.lang.parser.ICapturedSymbol;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.IStatement;
import gw.lang.parser.ISymbol;
import gw.lang.parser.Keyword;
import gw.lang.parser.StandardSymbolTable;
import gw.lang.reflect.FunctionType;
import gw.lang.reflect.IFunctionType;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.GosuClassTypeLoader;
import gw.lang.reflect.gs.ICompilableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockClass extends SyntheticClass implements IBlockClassInternal
{
  private static final AtomicInteger TMP_INT_IDENTIFIER = new AtomicInteger( 0 );
  private BlockClass( BlockExpression blk )
  {
    super( "_todo_remove_me", "_todo_remove_me." + GosuClassTypeLoader.BLOCK_PREFIX + TMP_INT_IDENTIFIER.incrementAndGet() + GosuClassTypeLoader.BLOCK_POSTFIX,
           TypeSystem.getTypeLoader( GosuClassTypeLoader.class ), null, CommonServices.INSTANCE.getEntityAccess().getDefaultTypeUses() );
    initCompilationState();
    createNewParseInfo();
    getParseInfo().setBlock( blk );
  }

  private BlockClass( ICompilableType enclosingClass, int i, BlockExpression blk )
  {
    super( enclosingClass.getName(), GosuClassTypeLoader.BLOCK_PREFIX + i + GosuClassTypeLoader.BLOCK_POSTFIX,
           enclosingClass.getTypeLoader(), enclosingClass.getSourceFileHandle(), enclosingClass.getTypeUsesMap() );
    createNewParseInfo();
    getParseInfo().setBlock( blk );
    setEnclosingType( enclosingClass );
    initCompilationState();
  }

  @Override
  public Map<String, ICapturedSymbol> getCapturedSymbols()
  {
    return getParseInfo().getBlock().getCapturedSymbols();
  }

  private void initType()
  {
    BlockExpression block = getBlock();
    if( block.getArgs().size() < IBlock.MAX_ARGS )
    {
      IType returnType = block.getBlockReturnType();
      IType functionClassForArity = FunctionClassUtil.getFunctionClassForArity( returnType != null && returnType != JavaTypes.pVOID(), block.getArgs().size());
      setSuperType(functionClassForArity);
    }
    else
    {
      // This is a bad block that will have errors, so just set up a super type with zero args
      setSuperType( FunctionClassUtil.getFunctionClassForArity( block.getReturnType() != JavaTypes.pVOID(), 0 ) );
    }
  }

  public void update()
  {
    initType();
    createNewParseInfo();
    getParseInfo().addDefaultConstructor( new StandardSymbolTable(), null );

    // implement the one method that blocks implement
    implementInvoke();

    implementToString();
  }

  private void implementInvoke()
  {
    BlockExpression block = getBlock();
    IParsedElement body = block.getBody();
    DynamicFunctionSymbol value;
    if( body instanceof Expression )
    {
      Expression expression = (Expression)body;
      if( block.getBlockReturnType() == JavaTypes.pVOID() )
      {
        ExpressionStatement exprStmt = new ExpressionStatement();
        exprStmt.setExpression( expression );
        exprStmt.setSynthetic( true );
        exprStmt.initLocation( expression.getLocation().getOffset(), expression.getLocation().getLength(),
                                     expression.getLineNum(), expression.getColumn(), expression.getLocation().getScriptPartId() );
        value = new DynamicFunctionSymbol( null, INVOKE_METHOD_NAME, convertToObjectSignature( block ), convertToObjectSymbols( block ), exprStmt );
      }
      else
      {
        ReturnStatement syntheticReturnStatement = new ReturnStatement();
        syntheticReturnStatement.setValue( expression );
        syntheticReturnStatement.initLocation( expression.getLocation().getOffset(), expression.getLocation().getLength(),
                                               expression.getLineNum(), expression.getColumn(), expression.getLocation().getScriptPartId() );
        value = new DynamicFunctionSymbol( null, INVOKE_METHOD_NAME, convertToObjectSignature( block ), convertToObjectSymbols( block ), syntheticReturnStatement );
      }
    }
    else
    {
      value = new DynamicFunctionSymbol( null, INVOKE_METHOD_NAME, convertToObjectSignature(block), convertToObjectSymbols(block), (IStatement)body );
    }
    value.setClassMember( true );
    value.setPublic( true );
    value.setFinal( true );
    getParseInfo().addMemberFunction(value);
  }

  private void implementToString()
  {
    Identifier thisId = new Identifier();
    thisId.setSymbol( new Symbol( Keyword.KW_this.getName(), this, null ), new StandardSymbolTable() );
    thisId.setType( this );

    BeanMethodCallExpression toStrCall = new BeanMethodCallExpression();
    toStrCall.setMethodDescriptor( JavaTypes.IBLOCK().getTypeInfo().getMethod( "toString" ) );
    toStrCall.setRootExpression( thisId );
    toStrCall.setType( JavaTypes.STRING() );

    ReturnStatement returnStmt = new ReturnStatement();
    returnStmt.setValue( toStrCall );

  }

  private IFunctionType convertToObjectSignature( BlockExpression blk )
  {
    IFunctionType functionType = blk.getType();
    IType[] iTypes = new IType[functionType.getParameterTypes().length];
    for( int i = 0; i < iTypes.length; i++ )
    {
      iTypes[i] = JavaTypes.OBJECT();
    }
    return new FunctionType( blk.getFunctionName(), blk.getBlockReturnType() == null  || blk.getBlockReturnType() == JavaTypes.pVOID()
                                                    ? JavaTypes.pVOID()
                                                    : JavaTypes.OBJECT(), iTypes );
  }

  @Override
  public void addCapturedSymbol( ICapturedSymbol sym )
  {
    getBlock().addCapturedSymbol(sym);
  }

  @Override
  public IType getEnclosingNonBlockType() {
    ICompilableTypeInternal type = getEnclosingType();
    while( type instanceof IBlockClassInternal )
    {
      type = type.getEnclosingType();
    }
    return type.getEnclosingNonBlockType();
  }

  private List<ISymbol> convertToObjectSymbols( BlockExpression blk )
  {
    List<ISymbol> syms = new ArrayList<ISymbol>();
    for( ISymbol iSymbol : blk.getArgs() )
    {
      Symbol symbol = new Symbol( (Symbol)iSymbol );
      symbol.setType( JavaTypes.OBJECT() );
      syms.add( symbol );
    }
    return syms;
  }

  public BlockExpression getBlock()
  {
    return getParseInfo().getBlock();
  }

  @Override
  public IType getBlockType()
  {
    return getBlock().getType();
  }

  public static IBlockClassInternal create( ICompilableTypeInternal enclosingClass, BlockExpression block, boolean staticBlock )
  {
    BlockClass blockClass;
    if( enclosingClass != null )
    {
      blockClass = new BlockClass( enclosingClass, enclosingClass.getBlockCount(), block );
    }
    else
    {
      blockClass = new BlockClass( block );
    }
    if( staticBlock )
    {
      blockClass.markStatic();
    }
    return (IBlockClassInternal)blockClass.getOrCreateTypeReference();
  }

  @Override
  public boolean isAnonymous()
  {
    return true;
  }
}
