package gw.lang.gosuc.simple;

import gw.lang.reflect.TypeSystem;

import java.io.File;
import java.util.StringTokenizer;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public abstract class BaseCompilerOutputWriter<TCompilationUnit, TResult extends CompilationResult<TCompilationUnit>> implements ICompilationOutputWriter<TResult> {
  protected final ICompilerDriver _driver;

  protected BaseCompilerOutputWriter(ICompilerDriver driver) {
    _driver = driver;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void createOutputFiles(TResult result) {
    var moduleOutputDirectory = TypeSystem.getGlobalModule().getOutputPath();

    if (moduleOutputDirectory == null) {
      throw new RuntimeException("Can't make class file, no output path defined.");
    }

    for (var gsClass : result.getTypes()) {
      try {
        var outRelativePath = getRelativePath(gsClass);
        var child = new File(moduleOutputDirectory.getPath().getFileSystemPathString());
        child.mkdirs();

        for (var tokenizer = new StringTokenizer(outRelativePath, File.separator + "/"); tokenizer.hasMoreTokens(); ) {
          var token = tokenizer.nextToken();
          child = new File(child, token);

          if (!child.exists()) {
            if (token.endsWith(".class")) {
              child.createNewFile();
            } else {
              child.mkdir();
            }
          }
        }

        populateClassFile(child, gsClass);
        onClassFilePopulated(child.getParentFile(), gsClass);
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

  protected abstract String getRelativePath(TCompilationUnit unit);

  protected abstract void populateClassFile(File classFile, TCompilationUnit unit);

  protected void onClassFilePopulated(File parentFile, TCompilationUnit unit) { }
}
