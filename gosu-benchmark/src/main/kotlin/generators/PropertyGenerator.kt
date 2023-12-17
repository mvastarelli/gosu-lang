package org.gosu.benchmark.generators

class PropertyGenerator {
    fun makeProperties(numProperties: Int): String {
        val properties = (1..numProperties).map(::makeRandomType)
        return properties.joinToString("\n")
    }

    private fun makeRandomType(num: Int) : String {
        val boolValues = listOf("true", "false")
        val readonlyFlag = listOf("readonly", "")

        val generators = listOf(
            { -> "Int as ${readonlyFlag.random()} Property$num = $num" },
            { -> "String as ${readonlyFlag.random()} Property$num = \"Property$num\"" },
            { -> "Boolean as ${readonlyFlag.random()} Property$num = ${boolValues.random()}" },
            { -> "Double as ${readonlyFlag.random()} Property$num = $num.toDouble()" },
            { -> "Float as ${readonlyFlag.random()} Property$num = $num.toFloat()" },
            { -> "Long as ${readonlyFlag.random()} Property$num = $num.toLong()" },
            { -> "Short as ${readonlyFlag.random()} Property$num = $num.toShort()" }
        )

        val typeGenerator = generators.random()
        return "  var _property$num : ${typeGenerator()}"
    }
}
