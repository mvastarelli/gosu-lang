package gw.lang.gosuc.simple;

import gw.fs.IDirectory;
import gw.lang.reflect.TypeSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public abstract class BaseCompilerOutputWriter<TCompilationUnit, TResult extends CompilationResult<TCompilationUnit>> implements ICompilationOutputWriter<TResult> {
  protected final ICompilerDriver _driver;

  protected BaseCompilerOutputWriter(ICompilerDriver driver) {
    _driver = driver;
  }

  @Override
  public void createOutputFiles(TResult result) {
    var moduleOutputDirectory = TypeSystem.getGlobalModule().getOutputPath();

    if (moduleOutputDirectory == null) {
      throw new RuntimeException("Can't make class file, no output path defined.");
    }

    for (var unit : result.getTypes()) {
      var outRelativePath = getRelativePath(unit);
      var classFullPath = getClassFullPath(moduleOutputDirectory, outRelativePath);

      try {
        makeClassFullPath(classFullPath);
        populateClassFile(classFullPath, unit);
        onClassFilePopulated(classFullPath.getParentFile(), unit);
      } catch (Throwable e) {
        _driver.sendCompileIssue(
                classFullPath,
                ERROR,
                0,
                0,
                0,
                String.format("Cannot create .class files.%n%s", Utils.getStackTrace(e)));
      }
    }
  }

  private static File getClassFullPath(IDirectory moduleOutputDirectory, String outRelativePath) {
    return Paths.get(moduleOutputDirectory.getPath().getFileSystemPathString(), outRelativePath).toFile();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void makeClassFullPath(File modulePath) throws IOException {
    var parent = modulePath.getParentFile();

    if (!parent.exists()) {
      parent.mkdirs();
    }

    modulePath.createNewFile();
  }

  protected abstract String getRelativePath(TCompilationUnit unit);

  protected abstract void populateClassFile(File classFile, TCompilationUnit unit);

  protected void onClassFilePopulated(File parentFile, TCompilationUnit unit) { }
}
