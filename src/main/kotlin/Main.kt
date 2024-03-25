import analysis.TypeChecking
import codegen.CodeGenerator
import lexer.Lexer
import parser.Parser
import parser.ast.TConstructor
import runtime.*
import kotlin.math.pow
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

            put(
                "println",
                buildSet {
                    for (type in listOf("Int", "Double", "Boolean", "String", "Unit", "Nothing?")) {
                        add(
                            TConstructor(
                                "Function1",
                                listOf(
                                    TConstructor(type),
                                    TConstructor("Unit")
                                ),
                            )
                        )
                    }
                    add(
                        TConstructor(
                            "Function0",
                            listOf(
                                TConstructor("Unit")
                            ),
                        )
                    )
                }
            )

            put(
                "print",
                buildSet {
                    for (type in listOf("Int", "Double", "Boolean", "String", "Unit", "Nothing?")) {
                        add(
                            TConstructor(
                                "Function1",
                                listOf(
                                    TConstructor(type),
                                    TConstructor("Unit")
                                ),
                            )
                        )
                    }
                }
            )

            put(
                "pow",
                setOf(
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

            put(
                "readInt",
                setOf(
                    TConstructor(
                        "Function1",
                        listOf(
                            TConstructor("Int"),
                        ),
                    ),
                )
            )
        }
    )
    val vm = VM()

    vm.addNativeFunction("println") {
        if (it.isNotEmpty()) {
            assert(it.size == 1)
            println(it[0])
            UnitValue
        } else {
            println()
            UnitValue
        }
    }

    vm.addNativeFunction("print") {
        assert(it.size == 1)
        print(it[0])
        UnitValue
    }

    vm.addNativeFunction("pow") {
        assert(it.size == 2)
        val (a, b) = it
        assert(a.value is Double)
        assert(b.value is Double)

        val av = a.value as Double
        val bv = b.value as Double

        av.pow(bv).toValue()
    }

    vm.addNativeFunction("readInt") {
        assert(it.isEmpty())

        readln().toInt().toValue()
    }

    val srcString = """
        val x: Int = readInt();
        val y: Int = readInt();

        if (x > y) {
            print(x);
            print(" is greater than ");
            println(y);
        } else if (x < y) {
            print(x);
            print(" is less than ");
            print(y);
            println();
        } else {
            print(x);
            print(" is equal to ");
            println(y);
        }
        
        println(if (x == y) {
            x + y;
        } else {
            x * y;
        });
    """.trimIndent()

    val lexer = Lexer(srcString)
    val parser = Parser(lexer.tokens)
    val codegen = CodeGenerator()

    val tree = parser.parse()

    tree.forEach(::println)

    typechecker.check(tree)

    tree.forEach(::println)

    val chunk = codegen.generate(tree)

    vm.interpret(chunk.also { c ->
        println(c.disassemble("source string"))
        println("=== source string ===")
    })

//    var i = 0
//    while (true) {
//        i++
//        print("[$i]>>> ")
//        readlnOrNull()?.takeIf {
//            it != ":q"
//        }?.let {
//            try {
//                val lexer = Lexer(it)
//                val parser = Parser(lexer.tokens)
//                val codegen = CodeGenerator()
//
//                val tree = parser.parse()
//
//                tree.forEach(::println)
//
//                typechecker.check(tree)
//
//                tree.forEach(::println)
//
//                val chunk = codegen.generate(tree)
//
//                vm.interpret(chunk.also { c ->
//                    println(c.disassemble("repl $i"))
//                })
//            } catch (e: Exception) {
//                println("error: ${e.localizedMessage}")
////                e.printStackTrace()
//            } catch (e: NotImplementedError) {
//                println(e.localizedMessage)
//            }
//        } ?: break
//    }
}