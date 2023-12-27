/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser.expressions;

import gw.internal.gosu.parser.ParserBase;
import gw.internal.gosu.parser.TypeLord;
import gw.lang.parser.IGosuParser;
import gw.lang.parser.expressions.IConditionalExpression;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.java.JavaTypes;
import gw.config.CommonServices;


/**
 * The base class for conditional expressions with logical operators e.g., && || ==.
 * Models conditional expressions by encapsulating the left and right hand side
 * operands.
 */
public abstract class ConditionalExpression extends BinaryExpression implements IConditionalExpression
{
  private IMethodInfo _override;

  /**
   * Base constructor sets type to BooleanType.
   */
  public ConditionalExpression()
  {
    setType( JavaTypes.pBOOLEAN() );
  }

  public IMethodInfo getOverride()
  {
    return _override;
  }
  public void setOverride( IMethodInfo overrideMi )
  {
    _override = overrideMi;
  }

  public boolean isCompileTimeConstant()
  {
    return getLHS().isCompileTimeConstant() && getRHS().isCompileTimeConstant();
  }

  public static int compareNumbers( Object lhsValue, Object rhsValue, IType lhsType, IType rhsType )
  {
    if( JavaTypes.COMPARABLE().isAssignableFrom( lhsType ) && lhsType == rhsType )
    {
      return ((Comparable)lhsValue).compareTo( rhsValue );
    }
    
    lhsType = ParserBase.resolveType( lhsType, '>', rhsType );
    try
    {
      if( JavaTypes.IDIMENSION().isAssignableFrom( lhsType ) ) {
        DimensionOperandResolver customNumberResolver =
          DimensionOperandResolver.resolve( lhsType, lhsType, lhsValue, rhsType, rhsValue );
        lhsType = customNumberResolver.getRawNumberType();
        lhsValue = customNumberResolver.getLhsValue();
        rhsValue = customNumberResolver.getRhsValue();
      }

      if( lhsType == JavaTypes.BIG_DECIMAL() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeBigDecimalFrom( lhsValue ).compareTo(CommonServices.INSTANCE.getCoercionManager().makeBigDecimalFrom( rhsValue ) );
      }

      if( lhsType == JavaTypes.BIG_INTEGER() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeBigIntegerFrom( lhsValue ).compareTo(CommonServices.INSTANCE.getCoercionManager().makeBigIntegerFrom( rhsValue ) );
      }

      if( lhsType == JavaTypes.INTEGER() || lhsType == JavaTypes.pINT() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( rhsValue ) );
      }
      if( lhsType == JavaTypes.LONG() || lhsType == JavaTypes.pLONG() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeLongFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeLongFrom( rhsValue ) );
      }
      if( lhsType == JavaTypes.DOUBLE() || lhsType == JavaTypes.pDOUBLE() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeDoubleFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeDoubleFrom( rhsValue ) );
      }
      if( lhsType == JavaTypes.FLOAT() || lhsType == JavaTypes.pFLOAT() )
      {
        float f1 = CommonServices.INSTANCE.getCoercionManager().makePrimitiveFloatFrom( lhsValue );
        float f2 = CommonServices.INSTANCE.getCoercionManager().makePrimitiveFloatFrom( rhsValue );
        return f1 > f2 ? 1 : f1 < f2 ? -1 : 0;
      }
      if( lhsType == JavaTypes.SHORT() || lhsType == JavaTypes.pSHORT() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( rhsValue ) );
      }
      if( lhsType == JavaTypes.BYTE() || lhsType == JavaTypes.pBYTE() )
      {
        return CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeIntegerFrom( rhsValue ) );
      }
    }
    catch( NumberFormatException nfe )
    {
      rhsValue = IGosuParser.ZERO;
    }
    return CommonServices.INSTANCE.getCoercionManager().makeDoubleFrom( lhsValue ).compareTo( CommonServices.INSTANCE.getCoercionManager().makeDoubleFrom( rhsValue ) );
  }

  @Override
  public IType getType()
  {
    return JavaTypes.pBOOLEAN();
  }

  @Override
  protected IType getTypeImpl()
  {
    return JavaTypes.pBOOLEAN();
  }
}
