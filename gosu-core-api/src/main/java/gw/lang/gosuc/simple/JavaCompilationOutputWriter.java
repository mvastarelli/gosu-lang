package gw.lang.gosuc.simple;

import manifold.internal.javac.InMemoryClassJavaFileObject;

import java.io.*;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class JavaCompilationOutputWriter extends BaseCompilerOutputWriter<InMemoryClassJavaFileObject, JavaCompilationResult> {
  public JavaCompilationOutputWriter(ICompilerDriver driver) {
    super(driver);
  }

  @Override
  protected String getRelativePath(InMemoryClassJavaFileObject unit) {
    return unit.getClassName().replace('.', File.separatorChar) + ".class";
  }

  @Override
  protected void populateClassFile(File classFile, InMemoryClassJavaFileObject unit) {
    var bytes = unit.getBytes();

    try( OutputStream out = new FileOutputStream(classFile) )
    {
      out.write( bytes );
      _driver.registerOutput( null, classFile);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      _driver.sendCompileIssue(null, ERROR, 0, 0, 0, String.format("Cannot write to .class file: %s", e.getMessage()));
    }
  }
}
