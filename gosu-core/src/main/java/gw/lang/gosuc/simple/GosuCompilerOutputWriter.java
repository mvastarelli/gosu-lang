package gw.lang.gosuc.simple;

import gw.fs.IFile;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.ISourceFileHandle;

import java.io.*;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class GosuCompilerOutputWriter extends BaseCompilerOutputWriter<IType, GosuCompilationResult> {
  private final File _sourceFile;

  public GosuCompilerOutputWriter(ICompilerDriver driver, File sourceFile) {
    super(driver);
    _sourceFile = sourceFile;
  }

  @Override
  protected String getRelativePath(IType unit) {
    return unit.getName().replace('.', File.separatorChar) + ".class";
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  protected void populateClassFile(File classFile, IType unit) {
    var gosuClass = (IGosuClass)unit;
    var bytes = TypeSystem.getGosuClassLoader().getBytes(gosuClass);

    try (OutputStream out = new FileOutputStream(classFile)) {
      out.write(bytes);
      _driver.registerOutput(_sourceFile, classFile);
    } catch (FileNotFoundException e) {
      _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, "Cannot find .class file");
      return;
    } catch (IOException e) {
      _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, String.format("Cannot write to .class file: %s", e.getMessage()));
      return;
    }

    for (IGosuClass innerClass : gosuClass.getInnerClasses()) {
      var innerClassName = String.format("%s$%s.class", classFile.getName().substring(0, classFile.getName().lastIndexOf('.')), innerClass.getRelativeName());
      var innerClassFile = new File(classFile.getParent(), innerClassName);

      if (innerClassFile.isFile()) {
        try {
          innerClassFile.createNewFile();
        } catch (IOException e) {
          _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, String.format("Cannot write inner class to .class file: %s", e.getMessage()));
          return;
        }
      }

      populateClassFile(innerClassFile, innerClass);
    }
  }

  @SuppressWarnings("CallToPrintStackTrace")
  @Override
  protected void onClassFilePopulated(File parent, IType unit) {
    var gsClass = (IGosuClass)unit;
    ISourceFileHandle sfh = gsClass.getSourceFileHandle();
    IFile srcFile = sfh.getFile();

    if (srcFile != null) {
      File file = new File(srcFile.getPath().getFileSystemPathString());
      if (file.isFile()) {
        try {
          File destFile = new File(parent, file.getName());
          copyFile(file, destFile);
          _driver.registerOutput(_sourceFile, destFile);
        } catch (IOException e) {
          e.printStackTrace();
          _driver.sendCompileIssue(_sourceFile, ERROR, 0, 0, 0, "Cannot copy source file to output folder.");
        }
      }
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void copyFile(File sourceFile, File destFile ) throws IOException
  {
    if( sourceFile.isDirectory() )
    {
      destFile.mkdirs();
      return;
    }

    if( !destFile.exists() )
    {
      destFile.getParentFile().mkdirs();
      destFile.createNewFile();
    }

    try(var fpIn = new FileInputStream( sourceFile );
        var fpOut = new FileOutputStream(destFile))
    {
      var source = fpIn.getChannel();
      var destination = fpOut.getChannel();

      destination.transferFrom( source, 0, source.size() );
    }
  }
}
