package gw.lang.gosuc.simple;

import gw.fs.FileFactory;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.module.IModule;

import java.io.File;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class GosuSourceCompiler implements ISourceCompiler<GosuCompilationResult> {
  private final static IType doNotVerifyAnnotation = TypeSystem.getByFullNameIfValid("gw.testharness.DoNotVerifyResource");
  private final static IModule globalModule = TypeSystem.getGlobalModule();

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
          return GosuCompilationResult.success(type);
        }
      } catch (CompilerDriverException ex) {
        _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, ex.getMessage());
        return GosuCompilationResult.failed();
      }

      return GosuCompilationResult.failed(type);
    }

    return GosuCompilationResult.failed();
  }

  private IType getType( File file )
  {
    var ifile = FileFactory.instance().getIFile( file );
    var typesForFile = TypeSystem.getTypesForFile( globalModule, ifile );

    if( typesForFile.length != 0 )
    {
      return TypeSystem.getByFullNameIfValid( typesForFile[0], globalModule );
    }
    return null;
  }

  private boolean isCompilable( IType type )
  {
    return type instanceof IGosuClass && !type.getTypeInfo().hasAnnotation( doNotVerifyAnnotation );
  }
}
