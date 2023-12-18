package org.gosu.benchmark.benchmarks

import gw.lang.gosuc.cli.CommandLineOptions
import gw.lang.gosuc.simple.GosuCompiler
import gw.lang.gosuc.simple.ICompilerDriver
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.BenchmarkParams

@BenchmarkMode(Mode.AverageTime)
class CompilerBenchmark(
    private val plan: CompilerPlan
) {
    @Benchmark
    fun compile() {
        Thread.sleep(100)
    }
}

@State(Scope.Benchmark)
class CompilerPlan {
    private lateinit var compiler: GosuCompiler
    private lateinit var driver : ICompilerDriver
    private lateinit var options : CommandLineOptions

    @Setup(Level.Invocation)
    fun setup(parameters: BenchmarkParams) {
        println("CompilerPlan.setup")
    }

    @TearDown(Level.Invocation)
    fun teardown() {
        println("CompilerPlan.teardown")
        // compiler.uninitializeGosu()
    }
}