package org.gosu.samplegen

import gw.internal.ext.com.beust.jcommander.Parameter
import gw.internal.ext.com.beust.jcommander.validators.PositiveInteger

class CommandLineOptions {
    @Parameter(
        names = ["-path"],
        description = "Path to save the generated classes to",
        required =  true)
    var path: String = ""

    @Parameter(
        names = ["-classes"],
        description = "Number of classes to create",
        validateWith = [PositiveInteger::class])
    var numClasses: Int = 50

    @Parameter(
        names = ["-complexity"],
        description = "Complexity of the generated classes")
    var complexity: ClassComplexity = ClassComplexity.SIMPLE
}
