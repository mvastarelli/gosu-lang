package gw.benchmark;

import gw.lang.gosuc.cli.CommandLineOptions;
import gw.lang.gosuc.simple.GosuCompiler;
import gw.lang.gosuc.simple.ICompilerDriver;
import gw.lang.gosuc.simple.IGosuCompiler;
import gw.lang.gosuc.simple.SoutCompilerDriver;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.nio.file.FileSystems;

// @State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
public class CompilerBenchmarks {
  @Param("sourcePath")
  private static String sourcePath;

  @Param({"tempPath", "/tmp/gosu-benchmark/"})
  private static String tempPath;

  private IGosuCompiler _compiler;
  private ICompilerDriver _driver;
  private CommandLineOptions _options;

  public CompilerBenchmarks() {
    System.out.println("CompilerBenchmarks.CompilerBenchmarks");
  }

  @Setup
  public void setup() {
    System.out.println(String.format("Source path: %s", sourcePath));
    System.out.println(String.format("Temp path: %s", tempPath));

    var compiler = new GosuCompiler();
    var driver = new SoutCompilerDriver(false, false);
  }

  @TearDown
  public void tearDown() {
    System.out.println("CompilerBenchmarks.tearDown");
  }

  @TearDown(Level.Invocation)
  public void deleteTempDir() {
    System.out.println("CompilerBenchmarks.deleteTempDir");
  }

  @Benchmark
  public void benchmark() throws InterruptedException {
    Thread.sleep(100);
  }
}
