package org.gosu.samplegen.generators

class PropertyGenerator {
    fun makeProperties(numProperties: Int): String {
        val properties = (1..numProperties).map(::makeRandomType)
        return properties.joinToString("\n")
    }

    private fun makeRandomType(num: Int) : String {
        val boolValues = listOf("true", "false")
        val readonlyFlag = listOf("readonly", "")

        val generators = listOf(
            { -> "int as ${readonlyFlag.random()} Property$num = $num" },
            { -> "String as ${readonlyFlag.random()} Property$num = \"Property$num\"" },
            { -> "Boolean as ${readonlyFlag.random()} Property$num = ${boolValues.random()}" }
        )

        val typeGenerator = generators.random()
        return "  var _property$num : ${typeGenerator()}"
    }
}
