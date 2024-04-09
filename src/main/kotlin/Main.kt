import analysis.TypeChecker
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
    val typechecker = TypeChecker(
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

            put(
                "readDouble",
                setOf(
                    TConstructor(
                        "Function1",
                        listOf(
                            TConstructor("Double")
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

    vm.addNativeFunction("readDouble") {
        assert(it.isEmpty())

        readln().toDouble().toValue()
    }

   // val srcString = """
   //     var a: Int = 0;
   //     var b: Int = 1;
   //     val n: Int = readInt();
   //     // val test: Double = readDouble();
   //
   //     print("fib(");
   //     print(n);
   //     print(") = ");
   //     if (n == 0) {
   //         println(0);
   //     } else {
   //         var i: Int = 0;
   //         while (i < n - 1) {
   //             val tmp: Int = a + b;
   //             a = b;
   //             b = tmp;
   //             i = i + 1;
   //         }
   //
   //         println(b);
   //     }
   //     println(pow(2.0, 8.5));
   //     // println(test);
   // """.trimIndent()

   // val srcString = """
   //     fun foo(bar: Int, baz: Boolean): String {
   //         if (baz) {
   //             return "test";
   //         } else {
   //             return "hello world";
   //         }
   //     }
   //
   //     fun test() {
   //         println(foo(10, false));
   //         return;
   //     }
   //
   //     test();
   // """.trimIndent()

    // val srcString = """
    //     fun greeting(): String {
    //         return "Hello World";
    //     }
    //
    //     fun test(input: String) {
    //         println(input);
    //     }
    //
    //     println(test(greeting()));
    //     println("test");
    // """.trimIndent()

    val srcString = """
        fun fib(n: Int): Int {
            if (n == 0 || n == 1) {
                return n;
            } else {
                return fib(n - 1) + fib(n - 2);
            }
        }
        
        print("n = ");
        val n: Int = readInt();
        
        print("fib(");
        print(n);
        print(") = ");
        println(fib(n));
    """.trimIndent()

    val lexer = Lexer(srcString)
    val parser = Parser(lexer.tokens)
    val codegen = CodeGenerator()

    val tree = parser.parse()

    tree.forEach(::println)

    typechecker.check(tree)

    tree.forEach(::println)

//    println(typechecker.environment["foo"])
//    println(typechecker.environment["test"])

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