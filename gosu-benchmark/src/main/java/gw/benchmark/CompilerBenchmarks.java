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
@Warmup(iterations = 1)
@Fork(1)
@Measurement(iterations = 2)
@BenchmarkMode(Mode.AverageTime)
public class CompilerBenchmarks {
  @State(Scope.Benchmark)
  public static class CompilerState {
    @Param("sourcePath")
    public static String sourcePath;

    @Param("tempPath")
    public static String tempPath;

    public IGosuCompiler compiler;
    public ICompilerDriver driver;
    public CommandLineOptions options;

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
      //System.out.println("CompilerBenchmarks.deleteTempDir");
    }
  }

  @Benchmark
  public void benchmark(CompilerState state) throws InterruptedException {
    System.out.println("Invoking benchmark, sourcePath: " + state.sourcePath + ", tempPath: " + state.tempPath);
    Thread.sleep(1000);
  }
}
