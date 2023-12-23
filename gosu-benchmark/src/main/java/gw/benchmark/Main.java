package gw.benchmark;

import gw.internal.ext.com.beust.jcommander.JCommander;
import gw.internal.ext.com.beust.jcommander.ParameterException;
import gw.lang.gosuc.simple.GosuCompiler;
import gw.lang.gosuc.simple.SoutCompilerDriver;

import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

public class Main {
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void main(String[] args) throws IOException {
    var options = new CommandLineOptions();
    var sourcePath = makeTempFolder();
    var tempPath = makeTempFolder();

    try {
      JCommander.newBuilder()
              .programName("gosu-benchmark")
              .addObject(options)
              .args(args)
              .build();

      System.out.println(String.format("Source path: %s", sourcePath));
      System.out.println(String.format("Temp path: %s", tempPath));

      System.out.println("Extracting sources...");
      extractSources(sourcePath);

      System.out.println("Running benchmark...");
      runBenchmark(sourcePath, tempPath);
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      e.usage();
      System.exit(1);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static String makeTempFolder() throws IOException {
    var tempFolder = File.createTempFile("gosu-benchmark", "");
    tempFolder.delete();
    tempFolder.mkdir();
    tempFolder.deleteOnExit();
    return tempFolder.getAbsolutePath();
  }

  private static void extractSources(String tempPath) throws IOException {
    var stream = Main.class.getClassLoader().getResourceAsStream("gosu-benchmark.zip");

    try (stream) {
      if (stream == null) {
        throw new RuntimeException("Could not find gosu-benchmark.zip in resources.");
      }
      var zipStream = new ZipInputStream(stream);
      var entry = zipStream.getNextEntry();

      while (entry != null) {
        var path = Paths.get(tempPath, entry.getName());
        var parent = path.getParent();

        if (parent != null) {
          Files.createDirectories(parent);
        }

        if (!entry.isDirectory()) {
          Files.copy(zipStream, path);
        }

        zipStream.closeEntry();
        entry = zipStream.getNextEntry();
      }
    }
  }

  @SuppressWarnings("resource")
  private static void runBenchmark(String sourcePath, String tempPath) throws IOException {
    var compiler = new GosuCompiler();
    var driver = new SoutCompilerDriver(false, false);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:**/*");

    var files = Files.walk(new File(sourcePath).toPath())
            .map(Path::toAbsolutePath)
            .filter(matcher::matches)
            .filter(path -> !Files.isDirectory(path))
            .map(Path::toString)
            .collect(Collectors.toList());

    var classPath = Arrays.asList(System.getProperties()
            .getProperty("java.class.path")
            .split(File.pathSeparator));

    var cliOptions = new gw.lang.gosuc.cli.CommandLineOptions();

    cliOptions.setMaxErrs(Integer.MAX_VALUE);
    cliOptions.setNoWarn(true);
    cliOptions.setVerbose(false);
    cliOptions.setSourcepath(sourcePath);
    cliOptions.setSourceFiles(files);

    System.out.println("Initializing compiler...");
    compiler.initializeGosu(
            Collections.singletonList(sourcePath),
            classPath,
            tempPath);

    System.out.println("Compiling...");
    var thresholdExceeded = compiler.compile(cliOptions, driver);

    if(thresholdExceeded) {
      System.out.println("Compilation failed.");
    } else {
      System.out.println("Compilation succeeded.");
    }

    System.out.println("Cleaning up...");
    compiler.uninitializeGosu();

    System.exit(thresholdExceeded ? 0 : 1);
  }
}
