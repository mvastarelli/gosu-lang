package org.gosu.samplegen.generators

import org.gosu.samplegen.GeneratedClass

class ClassGenerator(
    private val numMethods: Int,
    private val numProperties: Int,
    private val methodVariance: Double,
    private val propertyVariance: Double,
    private val packageName: String
) {
    val makeProperties = PropertyGenerator()::makeProperties
    val makeMethods = MethodGenerator()::makeMethods

    fun makeClass(): GeneratedClass {
        val actualNumMethods =  randomizeVariance(numMethods, methodVariance)
        val actualNumProperties = randomizeVariance(numProperties, propertyVariance)
        val className = Randomizer.className()

        val source = """package $packageName

public class ${className} {
${makeProperties(actualNumProperties)}
${makeMethods(actualNumMethods)}
}
"""

        return GeneratedClass(className, source)
    }

    private fun randomizeVariance(value: Int, variance: Double): Int {
        val varianceAmount = (value * (variance/100.0)).toInt()
        val varianceRange = (0..varianceAmount)
        val varianceDirection = (-1..1).random()

        return value + varianceRange.random() * varianceDirection
    }
}

