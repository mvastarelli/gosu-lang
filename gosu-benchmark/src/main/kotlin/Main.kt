package org.gosu.benchmark

import gw.internal.ext.com.beust.jcommander.JCommander
import gw.internal.ext.com.beust.jcommander.ParameterException
import gw.lang.gosuc.simple.GosuCompiler
import gw.lang.gosuc.simple.SoutCompilerDriver
import org.gosu.benchmark.generators.ClassGenerator
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.system.exitProcess
import gw.lang.gosuc.cli.CommandLineOptions as GosuCommandLineOptions

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
            generateClasses(options.numClasses, options.complexity, options.path, options.complexity.toString())
        }

        if(options.skipBenchmark) {
            println("Skipping benchmarking")
            exitProcess(0)
        }
        else {
            runBenchmarks(options.path, options.sourceFiles, options.tempPath)
        }
    } catch(e: ParameterException) {
        System.err.println(e.message)
        e.usage()
        exitProcess(1)
    }
}

fun generateClasses(numClasses: Int, complexity: ClassComplexity, path: String, packageName: String) {
    val preset = complexityPresets[complexity] ?: throw IllegalArgumentException("Invalid complexity preset: $complexity")
    val savePath = Path(path).toAbsolutePath()
    val clampedMethodVariance = clampVariance(preset.methodVariance)
    val clampedPropertyVariance = clampVariance(preset.propertyVariance)
    val makeClass = ClassGenerator(preset.methods, preset.properties, clampedMethodVariance, clampedPropertyVariance, packageName)::makeClass

    println("Generating $numClasses classes, ${preset.methods} methods, and ${preset.properties} properties")
    println("Saving to $savePath")
    println("Variances: ${clampedMethodVariance}% methods, ${clampedPropertyVariance}% properties")

    Paths.get(savePath.toString(), packageName).toFile().mkdirs()

    for (i in 1..numClasses) {
        val generatedClass = makeClass()
        val classPath = Paths.get(savePath.toString(), packageName, "${generatedClass.name}.gs")

        println("Saving ${generatedClass.name} to $classPath")
        classPath.toFile().writeText(generatedClass.source, Charsets.UTF_8)
    }
}

fun runBenchmarks(path: String, pattern: String, tempPath: String) {
    val compiler = GosuCompiler()
    val driver = SoutCompilerDriver(true, true)
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")

    val files = File(path)
        .walk()
        .filter{ matcher.matches(it.toPath()) }
        .map { f -> f.absolutePath }
        .toList()

    val classPath = System.getProperty("java.class.path").split(File.pathSeparator)
    val sourcePath = arrayOf(File(path).absolutePath).toList()

    val options = GosuCommandLineOptions().apply {
        maxErrs = 1
        isNoWarn = false
        isVerbose = false
        sourcepath = File(path).absolutePath
        sourceFiles = files
    }

    println("Running benchmarks")
    compiler.initializeGosu(sourcePath, classPath, tempPath )
    compiler.compile(options, driver)
    compiler.uninitializeGosu()
    println("Finished")
}

fun clampVariance(value: Int, maxVariance: Int=20): Double {
    if (value > maxVariance) {
        return maxVariance.toDouble()
    }

    return value.toDouble()
}
