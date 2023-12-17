package org.gosu.benchmark

import gw.internal.ext.com.beust.jcommander.Parameter
import gw.internal.ext.com.beust.jcommander.validators.PositiveInteger

class CommandLineOptions {
    @Parameter(
        names = ["-path"],
        description = "Path to save the generated classes to")
    var path: String = "resources"

    @Parameter(
        names = ["-classes"],
        description = "Number of classes to create",
        validateWith = [PositiveInteger::class])
    var numClasses: Int = 10

    @Parameter(
        names = ["-complexity"],
        description = "Complexity of the generated classes")
    var complexity: ClassComplexity = ClassComplexity.SIMPLE

    @Parameter(
        names = ["-noGenerate"],
        description = "Use the classes in the path instead of generating new ones")
    var noGenerate: Boolean = false

    @Parameter(
        names = ["-noBenchmark"],
        description = "Skip the benchmarking step")
    var skipBenchmark: Boolean = false
}
