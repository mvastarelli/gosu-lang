package gw.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;

public class Main {
  public static void main(String[] args) throws RunnerException {
      new Runner(new OptionsBuilder()
        .include(".*") //  + CompilerBenchmarks.class.getSimpleName() + ".*")
        .forks(1)
        .build()).run();
  }
}