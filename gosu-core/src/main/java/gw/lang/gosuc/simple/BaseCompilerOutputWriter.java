package gw.lang.gosuc.simple;

import gw.fs.IDirectory;
import gw.lang.reflect.TypeSystem;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

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
      try {
        var outRelativePath = getRelativePath(unit);
        var classFullPath = getClassFullPath(moduleOutputDirectory, outRelativePath);

        populateClassFile(classFullPath, unit);
        onClassFilePopulated(classFullPath.getParentFile(), unit);
      } catch (Throwable e) {
        _driver.sendCompileIssue(
                null,
                ERROR,
                0,
                0,
                0,
                String.format("Cannot create .class files.%n%s", Utils.getStackTrace(e)));
      }
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static File getClassFullPath(IDirectory moduleOutputDirectory, String outRelativePath) throws IOException {
    var modulePath = new File(moduleOutputDirectory.getPath().getFileSystemPathString());
    modulePath.mkdirs();

    for (var tokenizer = new StringTokenizer(outRelativePath, File.separator + "/"); tokenizer.hasMoreTokens(); ) {
      var token = tokenizer.nextToken();
      modulePath = new File(modulePath, token);

      if (!modulePath.exists()) {
        if (token.endsWith(".class")) {
          modulePath.createNewFile();
        } else {
          modulePath.mkdir();
        }
      }
    }

    return modulePath;
  }

  protected abstract String getRelativePath(TCompilationUnit unit);

  protected abstract void populateClassFile(File classFile, TCompilationUnit unit);

  protected void onClassFilePopulated(File parentFile, TCompilationUnit unit) { }
}
