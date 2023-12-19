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

public class Main {
  public static void main(String[] args) {
    var options = new CommandLineOptions();

    try {
      JCommander.newBuilder()
        .programName("gosu-benchmark")
        .addObject(options)
        .args(args)
        .build();

      runBenchmark(options);
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      e.usage();
      System.exit(1);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runBenchmark(CommandLineOptions options) throws IOException {
    var compiler = new GosuCompiler();
    var driver = new SoutCompilerDriver(false, false);
    var matcher = FileSystems.getDefault().getPathMatcher("glob:" + options.getFilter());

    var files = Files.walk(new File(options.getSourcePath()).toPath())
            .map(Path::toAbsolutePath)
            .filter(matcher::matches)
            .map(Path::toString)
            .collect(Collectors.toList());

    var classPath = Arrays.asList(System.getProperties()
            .getProperty("java.class.path")
            .split(File.pathSeparator));

    var sourcePath = new File(options.getSourcePath()).getAbsolutePath();
    var cliOptions = new gw.lang.gosuc.cli.CommandLineOptions();

    cliOptions.setMaxErrs(100);
    cliOptions.setNoWarn(true);
    cliOptions.setVerbose(false);
    cliOptions.setSourcepath(sourcePath);
    cliOptions.setSourceFiles(files);

    System.out.println("Initializing compiler...");
    compiler.initializeGosu(
            Collections.singletonList(sourcePath),
            classPath,
            options.getTempPath());

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
