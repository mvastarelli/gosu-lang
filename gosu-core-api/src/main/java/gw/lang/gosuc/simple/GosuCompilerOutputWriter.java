package gw.lang.gosuc.simple;

import gw.fs.IDirectory;
import gw.fs.IFile;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.ISourceFileHandle;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

import static gw.lang.gosuc.simple.ICompilerDriver.ERROR;

public class GosuCompilerOutputWriter implements ICompilationOutputWriter<GosuCompilationResult> {
  private final ICompilerDriver _driver;
  private final File _sourceFile;

  public GosuCompilerOutputWriter(ICompilerDriver driver, File sourceFile) {
    _driver = driver;
    _sourceFile = sourceFile;
  }

  @Override
  public void createOutputFiles(GosuCompilationResult result) {
    var type = result.getType();

    if (type.isEmpty()) {
      return;
    }

    var gsClass = ((IGosuClass) type.get());
    IDirectory moduleOutputDirectory = TypeSystem.getGlobalModule().getOutputPath();
    if (moduleOutputDirectory == null) {
      throw new RuntimeException("Can't make class file, no output path defined.");
    }

    final String outRelativePath = gsClass.getName().replace('.', File.separatorChar) + ".class";
    File child = new File(moduleOutputDirectory.getPath().getFileSystemPathString());
    child.mkdirs();

    try {
      for (StringTokenizer tokenizer = new StringTokenizer(outRelativePath, File.separator + "/"); tokenizer.hasMoreTokens(); ) {
        String token = tokenizer.nextToken();
        child = new File(child, token);
        if (!child.exists()) {
          if (token.endsWith(".class")) {
            child.createNewFile();
          } else {
            child.mkdir();
          }
        }
      }

      populateGosuClassFile(child, gsClass);
      maybeCopySourceFile(child.getParentFile(), gsClass, _sourceFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void populateGosuClassFile(File outputFile, IGosuClass gosuClass) throws IOException {
    final byte[] bytes = TypeSystem.getGosuClassLoader().getBytes(gosuClass);
    try (OutputStream out = new FileOutputStream(outputFile)) {
      out.write(bytes);
      _driver.registerOutput(_sourceFile, outputFile);
    }
    for (IGosuClass innerClass : gosuClass.getInnerClasses()) {
      final String innerClassName = String.format("%s$%s.class", outputFile.getName().substring(0, outputFile.getName().lastIndexOf('.')), innerClass.getRelativeName());
      File innerClassFile = new File(outputFile.getParent(), innerClassName);
      if (innerClassFile.isFile()) {
        innerClassFile.createNewFile();
      }
      populateGosuClassFile(innerClassFile, innerClass);
    }
  }

  private void maybeCopySourceFile(File parent, IGosuClass gsClass, File sourceFile) {
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
          _driver.sendCompileIssue(sourceFile, ERROR, 0, 0, 0, "Cannot copy source file to output folder.");
        }
      }
    }
  }

  public void copyFile( File sourceFile, File destFile ) throws IOException
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

    try(FileChannel source = new FileInputStream( sourceFile ).getChannel();
        FileChannel destination = new FileOutputStream( destFile ).getChannel() )
    {
      destination.transferFrom( source, 0, source.size() );
    }
  }
}
