package gw.lang.gosuc.simple;

import gw.fs.FileFactory;
import gw.fs.IFile;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.module.IModule;

import java.io.File;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class GosuSourceCompiler implements ISourceCompiler<GosuCompilationResult> {
  private final ICompilerDriver _driver;
  private final File _sourceFile;

  public GosuSourceCompiler(ICompilerDriver driver, File sourceFile) {
    _driver = driver;
    _sourceFile = sourceFile;
  }

  @Override
  public GosuCompilationResult compile() {
    IType type = getType(_sourceFile);
    if (type == null) {
      _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, "Cannot find type in the Gosu Type System.");
      return GosuCompilationResult.failed();
    }

    if (isCompilable(type)) {
      try {
        if (type.isValid()) {
          // createGosuOutputFiles((IGosuClass) type);
          return GosuCompilationResult.success(type);
        }
      } catch (CompilerDriverException ex) {
        _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, ex.getMessage());
        return GosuCompilationResult.failed();
      }
    }

    return GosuCompilationResult.failed();
  }

  private IType getType( File file )
  {
    IFile ifile = FileFactory.instance().getIFile( file );
    IModule module = TypeSystem.getGlobalModule();
    String[] typesForFile = TypeSystem.getTypesForFile( module, ifile );
    if( typesForFile.length != 0 )
    {
      return TypeSystem.getByFullNameIfValid( typesForFile[0], module );
    }
    return null;
  }

  private boolean isCompilable( IType type )
  {
    IType doNotVerifyAnnotation = TypeSystem.getByFullNameIfValid( "gw.testharness.DoNotVerifyResource" );
    return type instanceof IGosuClass && !type.getTypeInfo().hasAnnotation( doNotVerifyAnnotation );
  }
}
