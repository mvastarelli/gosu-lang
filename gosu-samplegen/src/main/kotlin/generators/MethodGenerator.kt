package org.gosu.samplegen.generators

import kotlin.random.Random

fun Int.random(min: Int = 0): Int {
    return Random.nextInt(min, this)
}

fun interface MakeSection {
    fun make(indent: Int, depth: Int): String
}

class MethodGenerator(
    private val maxDepth: Int = 3
) {
    companion object {
        fun pad(indent: Int): String {
            return " ".repeat(indent)
        }
    }

    fun makeMethods(numMethods: Int): String {
        val methods =  (1..numMethods).map (fun(_: Int) : String {
            val numVariables = Random.nextInt(1, 5)
            val numSections = Random.nextInt(1, 5)

            return makeMethod(numVariables, numSections)
        })

        return methods.joinToString("\n")
    }

    private fun makeMethod(numVariables: Int, numSections: Int): String {
        return """
  function ${Randomizer.methodName()}() : int {
${(1..numVariables).joinToString("\n") { _ -> makeVariable() }}

${(1..numSections).joinToString("\n") { _ -> makeSection() }}

    return ${Random.nextInt(1, 100)}
  }"""
    }

    private fun makeVariable(indent: Int = 4): String {
        val defaultValues = listOf(
            { listOf("\"true\"", "\"false\"").random() },
            { Random.nextInt(1, 100).toString() },
            { Random.nextDouble(1.0, 100.0).toString() },
            { "\"${Randomizer.variableName()}\"" }
        )

        return "${pad(indent)}var ${Randomizer.variableName()} = ${defaultValues.random().invoke()}"
    }

    private fun makeSection(indent : Int = 4, depth: Int = this.maxDepth): String {
        val maxIfBranches = Random.nextInt(1,3)

        val blockSections = listOf(
            { SectionGenerator.IfSectionGenerator.make(maxIfBranches, indent, depth, this::makeSection) },
            { SectionGenerator.ForSectionGenerator.make(indent, depth, this::makeSection) }
        )

        val leafSections = listOf(
            { "${pad(indent)}print(\"Hello World\")"},
            { "${pad(indent)}throw \"Error\""}
        )

        if(depth <= 0) {
            return leafSections.random().invoke()
        }

        return blockSections.random().invoke()
    }

    class SectionGenerator {
        class IfSectionGenerator {
            companion object {
                private const val MAX_CLAUSES = 3

                fun make(maxBranches: Int, indent: Int, depth: Int, makeSection: MakeSection ): String {
                    val ifStatement = """${pad(indent)}if( ${makeClauseCollection(MAX_CLAUSES.random(1))} ) {
${makeSection.make(indent + 2, depth - 1)}
"""

                    val elseifStatements = (1..maxBranches - 2).joinToString("") { _ ->
                        """${pad(indent)}} else if( ${makeClauseCollection(MAX_CLAUSES.random(1))} ) {
${makeSection.make(indent + 2, depth - 1)}
"""
                    }

                    val elseStatement =  if(maxBranches>1) {
                        """${pad(indent)}} else {
${makeSection.make(indent + 2, depth - 1)}
${pad(indent)}}"""
                    } else {
                        "${pad(indent)}}"
                    }

                    return "${ifStatement}${elseifStatements}${elseStatement}"
                }

                private fun makeClauseCollection(numClauses: Int): String {
                    val clauses = (1..numClauses).map { makeClause() }
                    val combiners = arrayOf(" and ", " or " )

                    val combined = clauses
                        .joinToString("$")
                        .replace(Regex.fromLiteral("$")) { _ -> combiners.random() }
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    return combined
                }

                private fun makeClause() : String {
                    val operators = arrayOf( "==", "!=", "<", ">", "<=", ">=" )
                    val booleanValues = arrayOf("true", "false")
                    val prefixes = arrayOf("", "not")

                    val clauses = arrayOf(
                        { booleanValues.random() },
                        { "(${booleanValues.random()} ${arrayOf("==", "!=").random()} ${booleanValues.random()})" },
                        { "(${Random.nextInt(1, 100)} ${operators.random()} ${Random.nextInt(1, 100)})" },
                    )

                    return "${prefixes.random()} ${clauses.random().invoke().trim()}"
                }
            }
        }

        class ForSectionGenerator {
            companion object {
                fun make(indent: Int, depth: Int, makeSection: MakeSection): String {
                    val forStatement =
"""${pad(indent)}for( ${makeForClause()} ) {
${makeSection.make(indent + 2, depth - 1)}
${pad(indent)}}"""

                    return forStatement
                }

                private fun makeForClause(): String {
                    val start = Random.nextInt(1, 10)
                    val stop = start + Random.nextInt(1, 10)

                    return "${Randomizer.shortVariableName()} in ${start}..${stop}"
                }
            }
        }
    }
}
