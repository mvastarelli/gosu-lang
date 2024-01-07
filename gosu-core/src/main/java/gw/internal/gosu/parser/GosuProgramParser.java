/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.internal.gosu.parser.expressions.EvalExpression;
import gw.internal.gosu.template.TemplateTokenizerInstructor;
import gw.lang.parser.*;
import gw.lang.parser.exceptions.ParseResultsException;
import gw.lang.parser.statements.IClassStatement;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.*;
import gw.lang.reflect.module.IModule;
import gw.util.GosuStringUtil;

import java.util.List;

/**
 *
 */
public class GosuProgramParser implements IGosuProgramParser {
  private static int g_iIndex;

  public static String makeEvalKey(String strSource, IType enclosingClass, IParsedElement ctxElem) {
    return makeEvalKey(strSource, enclosingClass, getEvalExprLocationOffset(ctxElem));
  }

  public static String makeEvalKey(String source, IType enclosingClass, int offset) {
    if (enclosingClass == null) {
      return "toplevel" + '.' + IGosuProgram.NAME_PREFIX + "eval_0_" + GosuStringUtil.getSHA1String(source);
    } else {
      return enclosingClass.getName() + '.' + IGosuProgram.NAME_PREFIX + "eval_" + offset + "_" + GosuStringUtil.getSHA1String(source);
    }
  }

  private static int getEvalExprLocationOffset(IParsedElement evalExpr) {
    if (evalExpr == null) {
      return getIndex();
    }
    IParseTree location = evalExpr.getLocation();
    return location != null ? location.getOffset() : getIndex();
  }

  synchronized static private int getIndex() {
    return g_iIndex++;
  }

  public IParseResult parseEval(String strSource, List<ICapturedSymbol> captured, IType enclosingClass, IParsedElement ctxElem, ISymbolTable extSyms) {
    TypeSystem.lock();
    try {
      IGosuProgramInternal program;
      String typeName = makeEvalKey(strSource, enclosingClass, ctxElem);
      if (ctxElem instanceof EvalExpression) {
        program = ((EvalExpression) ctxElem).getCachedProgram(typeName);
        if (program != null) {
          program.isValid();
          return new ParseResult(program);
        }
      }

      StringSourceFileHandle sfh = new StringSourceFileHandle(typeName, strSource, false, ClassType.Eval);
      if (enclosingClass != null) {
        sfh.setParentType(enclosingClass.getName());
      }
      ITypeUsesMap typeUsedMap = getTypeUsedMapFrom(ctxElem);
      if (typeUsedMap != null) {
        sfh.setTypeUsesMap(typeUsedMap);
      }
      if (ctxElem instanceof EvalExpression) {
        sfh.setCapturedTypeVars(((EvalExpression) ctxElem).getCapturedTypeVars());
      }
      program = (IGosuProgramInternal) GosuClassTypeLoader.getDefaultClassLoader().makeNewClass(sfh, null);
      program.setEnclosingEvalExpression(ctxElem);
      if (captured != null) {
        for (ICapturedSymbol sym : captured) {
          program.getParseInfo().addCapturedSymbolSilent(sym);
        }
      }
      sfh.setExternalSymbols(extSyms);
      program.isValid();
      if (ctxElem instanceof EvalExpression) {
        ((EvalExpression) ctxElem).cacheProgram(typeName, program);
      }
      return new ParseResult(program);
    } finally {
      TypeSystem.unlock();
    }
  }

  public IParseResult parseRuntimeExpr(String typeName, String strSource, IType enclosingClass, ISymbolTable extSyms, IParseTree ctxElem) {
    TypeSystem.lock();
    try {
      StringSourceFileHandle sfh = new StringSourceFileHandle(typeName, strSource, false, ClassType.Eval);
      sfh.setParentType(enclosingClass.getName());
      if (enclosingClass instanceof IGosuClass) {
        ITypeUsesMap typeUsedMap = ((IGosuClass) enclosingClass).getTypeUsesMap();
        if (typeUsedMap != null) {
          sfh.setTypeUsesMap(typeUsedMap);
        }
      }
      IGosuProgramInternal program = (IGosuProgramInternal) GosuClassTypeLoader.getDefaultClassLoader().makeNewClass(sfh, null);
      if (ctxElem != null) {
        program.setEnclosingEvalExpression(ctxElem.getParsedElement());
      }
      sfh.setExternalSymbols(extSyms);
      program.isValid();
      return new ParseResult(program);
    } finally {
      TypeSystem.unlock();
    }
  }

  private ITypeUsesMap getTypeUsedMapFrom(IParsedElement ctxElem) {
    if (ctxElem instanceof EvalExpression) {
      return ((EvalExpression) ctxElem).getTypeUsesMap();
    }
    IGosuClass gsClass = ctxElem.getGosuClass();
    if (gsClass != null) {
      return ((IGosuClass) TypeLord.getOuterMostEnclosingClass(gsClass)).getTypeUsesMap().copy();
    } else {
      return new TypeUsesMap();
    }
  }

  @Override
  public IParseResult parseExpressionOnly(String strSource, ISymbolTable symTable, ParserOptions options) throws ParseResultsException {
    return parseExpressionOrProgram(strSource, symTable, options);
  }

  @Override
  public IParseResult parseProgramOnly(String strSource, ISymbolTable symTable, ParserOptions options) throws ParseResultsException {
    return parseExpressionOrProgram(strSource, symTable, options);
  }

  @Override
  public IParseResult parseExpressionOrProgram(String strSource, ISymbolTable symTable, ParserOptions options) throws ParseResultsException {
    TypeSystem.lock();
    try {
      int index = getIndex();
      String name = null;
      IFileContext fileContext = options.getFileContext();
      if (fileContext != null) {
        name = fileContext.getClassName();
      }

      if (name == null) {
        name = IGosuProgram.PACKAGE + '.' + IGosuProgram.NAME_PREFIX + index;
      }

      if (fileContext != null && fileContext.getContextString() != null) {
        name += "_" + fileContext.getContextString();
      }

      StringSourceFileHandle sfh = new StringSourceFileHandle(name, strSource, false, ClassType.Program);
      if (fileContext != null) {
        sfh.setFilePath(fileContext.getFilePath());
      }
      sfh.setParentType(options.getEnclosingType());
      sfh.setTypeUsesMap(options.getTypeUsesMap());
      IGosuProgramInternal program = makeProgramClass(symTable, sfh);
      if (options.getParser() != null) {
        program.setEditorParser((GosuParser) options.getParser());
        program.setCreateEditorParser(options.getParser().isEditorParser());
      }
      program.setGenRootExprAccess(options.isBGenRootExprAccess());
      program.setExpectedReturnType(options.getExpectedType());
      program.setTokenizerInstructor(options.getTi());
      program.setSuperType(options.getSuperType());
      program.setAdditionalDFSDecls(options.getAdditionalDFSDecls());
      program.setStatementsOnly(options.isStatementsOnly());
      program.setThrowaway(options.isThrowawayProgram());
      program.setAnonymous(program.isAnonymous() || options.isAnonymous());
      program.setAllowUses(options.allowUses());
      program.setCtxInferenceMgr(options.getCtxInferenceMgr());
      if (options.getScriptPartId() != null) {
        program.setContextType(options.getScriptPartId().getContainingType());
      }

      if (!program.isValid()) {
        throw program.getParseResultsException();
      }

      if (options.getParser() != null) {
        try {
          IClassStatement classStatement = program.getClassStatement();
          IParsedElement parent = classStatement.getParent();
          IParsedElement pe = parent != null ? parent : classStatement;
          ((GosuParser) options.getParser()).verifyParsedElement(pe);
        } catch (ParseResultsException pre) {
          program.setParseResultsException(pre);
        }
      }

      return new ParseResult(program);
    } finally {
      TypeSystem.unlock();
    }
  }

  public IParseResult parseTemplate(String strSource, ISymbolTable symTable, ParserOptions options) throws ParseResultsException {
    TypeSystem.lock();
    try {
      int index = getIndex();
      String name = null;
      if (options.getFileContext() != null) {
        name = options.getFileContext().getClassName();
      }

      if (name == null) {
        name = IGosuProgram.PACKAGE + '.' + IGosuProgram.NAME_PREFIX + index;
      }

      if (options.getFileContext() != null && options.getFileContext().getContextString() != null) {
        name += "_" + options.getFileContext().getContextString();
      }

      StringSourceFileHandle sfh = new StringSourceFileHandle(name, strSource, false, ClassType.Template);
      if (options.getFileContext() != null) {
        sfh.setFilePath(options.getFileContext().getFilePath());
      }
      sfh.setTypeUsesMap(options.getTypeUsesMap());
      IGosuProgramInternal program = makeProgramClass(symTable, sfh);
      if (options.getParser() != null) {
        program.setEditorParser((GosuParser) options.getParser());
        program.setCreateEditorParser(options.getParser().isEditorParser());
      }
      program.setGenRootExprAccess(options.isBGenRootExprAccess());
      program.setExpectedReturnType(options.getExpectedType());
      program.setTokenizerInstructor(options.getTi());
      program.setSuperType(options.getSuperType());
      program.setAdditionalDFSDecls(options.getAdditionalDFSDecls());
      program.setStatementsOnly(options.isStatementsOnly());
      program.setThrowaway(options.isThrowawayProgram());
      program.setAllowUses(options.allowUses());
      program.setCtxInferenceMgr(options.getCtxInferenceMgr());
      if (options.getScriptPartId() != null) {
        program.setContextType(options.getScriptPartId().getContainingType());
      }
      program.setTokenizerInstructor(new TemplateTokenizerInstructor(options.getParser().getTokenizer()));

      if (!program.isValid()) {
        throw program.getParseResultsException();
      }

      if (options.getParser() != null) {
        try {
          IParsedElement pe = program.getClassStatement().getParent();
          pe = pe == null ? program.getClassStatement() : pe;
          ((GosuParser) options.getParser()).verifyParsedElement(pe);
        } catch (ParseResultsException pre) {
          program.setParseResultsException(pre);
        }
      }

      return new ParseResult(program);
    } finally {
      TypeSystem.unlock();
    }
  }

  private IGosuProgramInternal makeProgramClass(ISymbolTable symTable, StringSourceFileHandle sfh) {
    IModule module = TypeSystem.getCurrentModule();
    module = module == null ? TypeSystem.getGlobalModule() : module;
    GosuClassTypeLoader defaultClassLoader = TypeLoaderAccess.instance().getTypeLoader(GosuClassTypeLoader.class, module);
    //GosuClassTypeLoader defaultClassLoader = GosuClassTypeLoader.getDefaultClassLoader();
    return (IGosuProgramInternal) defaultClassLoader.makeNewClass(sfh, symTable);
  }
}
