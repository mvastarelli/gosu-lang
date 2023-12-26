/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.lang.reflect.java.asm;

import gw.internal.ext.org.objectweb.asm.Opcodes;
import gw.internal.ext.org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class MethodDeclarationSignatureVisitor extends SignatureVisitor {
  private final AsmMethod _asmMethod;
  private AsmType _csrTypeVar;
  private List<DeclarationPartSignatureVisitor> _paramVisitors;
  private DeclarationPartSignatureVisitor _returnVisitor;
  private List<DeclarationPartSignatureVisitor> _exceptionVisitors;

  MethodDeclarationSignatureVisitor( AsmMethod asmMethod ) {
    super( Opcodes.ASM7 );
    _asmMethod = asmMethod;
    _paramVisitors = Collections.emptyList();
    _exceptionVisitors = Collections.emptyList();
  }

  public List<DeclarationPartSignatureVisitor> getParamVisitors() {
    return _paramVisitors;
  }

  public DeclarationPartSignatureVisitor getReturnVisitor() {
    return _returnVisitor;
  }

  public List<DeclarationPartSignatureVisitor> getExceptionVisitors() {
    return _exceptionVisitors;
  }

  @Override
  public void visitFormalTypeParameter( String tv ) {
    _csrTypeVar = AsmUtil.makeTypeVariable( tv );
    _asmMethod.setGeneric();
    _csrTypeVar.setFunctionTypeVariable( true );
    _asmMethod.getMethodType().addTypeParameter( _csrTypeVar );
  }

  @Override
  public SignatureVisitor visitClassBound() {
    return new DeclarationPartSignatureVisitor( _csrTypeVar );
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    return new DeclarationPartSignatureVisitor( _csrTypeVar );
  }

  @Override
  public SignatureVisitor visitParameterType() {
    if( _paramVisitors.isEmpty() ) {
      _paramVisitors = new ArrayList<DeclarationPartSignatureVisitor>();
    }
    DeclarationPartSignatureVisitor visitor = new DeclarationPartSignatureVisitor( _asmMethod );
    _paramVisitors.add( visitor );
    return visitor;
  }

  @Override
  public SignatureVisitor visitReturnType() {
    _asmMethod.initGenericReturnType();
    return _returnVisitor = new DeclarationPartSignatureVisitor( _asmMethod );
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    if( _exceptionVisitors.isEmpty() ) {
      _exceptionVisitors = new ArrayList<DeclarationPartSignatureVisitor>();
    }
    DeclarationPartSignatureVisitor visitor = new DeclarationPartSignatureVisitor( _asmMethod );
    _exceptionVisitors.add( visitor );
    return visitor;
  }
}
