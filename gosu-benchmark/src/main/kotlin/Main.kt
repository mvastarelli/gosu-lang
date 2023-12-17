package org.gosu.benchmark

import gw.internal.ext.com.beust.jcommander.JCommander
import gw.internal.ext.com.beust.jcommander.ParameterException
import org.gosu.benchmark.generators.ClassGenerator
import kotlin.io.path.Path
import kotlin.system.exitProcess

val complexityPresets = mapOf(
    ClassComplexity.SIMPLE to ClassComplexityPreset(5, 3, 10, 10),
    ClassComplexity.COMPLEX to ClassComplexityPreset(15, 10, 20, 20),
    ClassComplexity.GORDIAN_KNOT to ClassComplexityPreset(25, 20, 25, 25)
)

fun main(args: Array<String>) {
    val options = CommandLineOptions()

    try {
        JCommander.newBuilder()
            .programName("gosu-benchmark")
            .addObject(options)
            .args(args)
            .build()

        if(!options.noGenerate) {
            generateClasses(options.numClasses, options.complexity, options.path)
        }

        if(options.skipBenchmark) {
            println("Skipping benchmarking")
            exitProcess(0)
        }
        else {
            runBenchmarks()
        }

    } catch(e: ParameterException) {
        System.err.println(e.message)
        e.usage()
        exitProcess(1)
    }
}

fun generateClasses(numClasses: Int, complexity: ClassComplexity, path: String) {
    val preset = complexityPresets[complexity] ?: throw IllegalArgumentException("Invalid complexity preset: $complexity")
    val savePath = Path(path).toAbsolutePath()
    val clampedMethodVariance = clampVariance(preset.methodVariance)
    val clampedPropertyVariance = clampVariance(preset.propertyVariance)
    val makeClass = ClassGenerator(preset.methods, preset.properties, clampedMethodVariance, clampedPropertyVariance)::makeClass

    println("Generating $numClasses classes, ${preset.methods} methods, and ${preset.properties} properties")
    println("Saving to $savePath")
    println("Variances: ${clampedMethodVariance}% methods, ${clampedPropertyVariance}% properties")

    for (i in 1..numClasses) {
        val generatedClass = makeClass()
        val classPath = savePath.resolve("${generatedClass.name}.gs")

        println("Saving ${generatedClass.name} to $classPath")
        classPath.toFile().writeText(generatedClass.source)
    }
}

fun runBenchmarks() {
    println("Running benchmarks")
}

fun clampVariance(value: Int, maxVariance: Int=20): Double {
    if(value > maxVariance) {
        return maxVariance.toDouble()
    }

    return value.toDouble()
}
