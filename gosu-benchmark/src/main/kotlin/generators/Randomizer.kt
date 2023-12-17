package org.gosu.benchmark.generators

class Randomizer {
    companion object {
        fun generateName(length: Int = 10): String {
            val upperChars = ('A' .. 'Z')
            val lowerChars = ('a' .. 'z')
            val allChars = lowerChars + upperChars

            return upperChars.random() +
                    (1..length)
                        .map { allChars.random() }
                        .joinToString("")
        }

        fun className(length: Int = 8): String {
            val upperChars = ('A' .. 'Z')
            val lowerChars = ('a' .. 'z')
            val allChars = lowerChars + upperChars

            return upperChars.random() +
                    (1..length-1)
                        .map { allChars.random() }
                        .joinToString("")
        }

        fun variableName(length: Int = 8): String {
            val upperChars = ('A' .. 'Z')
            val lowerChars = ('a' .. 'z')
            val allChars = lowerChars + upperChars

            return lowerChars.random() +
                    (1..length-1)
                        .map { allChars.random() }
                        .joinToString("")
        }

        fun shortVariableName(length: Int = 4) : String {
            return variableName(length)
        }

        fun methodName(length: Int = 8): String {
            return "${variableName(length)}_method"
        }
    }
}