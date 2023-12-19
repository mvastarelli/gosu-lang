package org.gosu.samplegen

import gw.internal.ext.com.beust.jcommander.JCommander
import gw.internal.ext.com.beust.jcommander.ParameterException
import org.gosu.samplegen.generators.ClassGenerator
import java.nio.file.Paths
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

        generateClasses(options.numClasses, options.complexity, options.path, options.complexity.toString())
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

        classPath.toFile().writeText(generatedClass.source, Charsets.UTF_8)
    }
}

fun clampVariance(value: Int, maxVariance: Int=20): Double {
    if (value > maxVariance) {
        return maxVariance.toDouble()
    }

    return value.toDouble()
}
