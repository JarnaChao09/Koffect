import analysis.TypeChecking
import codegen.CodeGenerator
import lexer.Lexer
import parser.Parser
import parser.ast.TConstructor
import runtime.*
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Flags currently unsupported")
        exitProcess(64)
    } else if (args.size == 1) {
        println("Running file currently unsupported")
        exitProcess(64)
    } else {
        repl()
    }
}

public fun repl() {
    val typechecker = TypeChecking(
        buildMap {
            for (function in listOf("plus", "minus", "times", "div", "mod")) {
                put(
                    function,
                    setOf(
                        TConstructor(
                            "Function2",
                            listOf(
                                TConstructor("Int"),
                                TConstructor("Int"),
                                TConstructor("Int"),
                            ),
                        ),
                        TConstructor(
                            "Function2",
                            listOf(
                                TConstructor("Double"),
                                TConstructor("Double"),
                                TConstructor("Double"),
                            ),
                        ),
                    )
                )
            }

            for (function in listOf("unaryPlus", "unaryMinus")) {
                put(
                    function,
                    setOf(
                        TConstructor(
                            "Function1",
                            listOf(
                                TConstructor("Int"),
                                TConstructor("Int")
                            ),
                        ),
                        TConstructor(
                            "Function1",
                            listOf(
                                TConstructor("Double"),
                                TConstructor("Double")
                            ),
                        ),
                    )
                )
            }

            for (function in listOf("==", "!=", ">=", "<=", ">", "<")) {
                put(
                    function,
                    setOf(
                        TConstructor(
                            "Function2",
                            listOf(
                                TConstructor("Int"),
                                TConstructor("Int"),
                                TConstructor("Boolean"),
                            ),
                        ),
                        TConstructor(
                            "Function2",
                            listOf(
                                TConstructor("Double"),
                                TConstructor("Double"),
                                TConstructor("Boolean"),
                            ),
                        ),
                    )
                )
            }

            for (function in listOf("&&", "||")) {
                put(
                    function,
                    setOf(
                        TConstructor(
                            "Function2",
                            listOf(
                                TConstructor("Boolean"),
                                TConstructor("Boolean"),
                                TConstructor("Boolean"),
                            ),
                        ),
                    )
                )
            }

            put(
                "not",
                setOf(
                    TConstructor(
                        "Function1",
                        listOf(
                            TConstructor("Boolean"),
                            TConstructor("Boolean",)
                        ),
                    ),
                )
            )
        }
    )
    val vm = VM()

    var i = 0
    while (true) {
        i++
        print("[$i]>>> ")
        readlnOrNull()?.takeIf {
            it != ":q"
        }?.let {
            try {
                val lexer = Lexer(it)
                val parser = Parser(lexer.tokens)
                val codegen = CodeGenerator()

                val tree = parser.parse()

                tree.forEach(::println)

                typechecker.check(tree)

                tree.forEach(::println)

                val chunk = codegen.generate(tree)

                vm.interpret(chunk.also { c ->
                    println(c.disassemble("repl $i"))
                })
            } catch (e: Exception) {
                println("error: ${e.localizedMessage}")
//                e.printStackTrace()
            } catch (e: NotImplementedError) {
                println(e.localizedMessage)
            }
        } ?: break
    }
}