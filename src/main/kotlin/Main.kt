import analysis.TypeChecker
import analysis.buildEnvironment
import codegen.CodeGenerator
import lexer.Lexer
import parser.Parser
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
    val env = buildEnvironment {
        function("println") {
            for (type in listOf("Int", "Double", "Boolean", "String", "Unit", "Nothing?")) {
                listOf(type) returns "Unit"
            }
            emptyList<String>() returns "Unit"
        }

        function("print") {
            for (type in listOf("Int", "Double", "Boolean", "String", "Unit", "Nothing?")) {
                listOf(type) returns "Unit"
            }
        }

        function("pow") {
            listOf("Double", "Double") returns "Double"
        }

        function("readInt") {
            emptyList<String>() returns "Int"
        }

        function("readDouble") {
            emptyList<String>() returns "Double"
        }

        for (type in listOf("Int", "Double")) {
            type {
                for (functionName in listOf("plus", "minus", "times", "div", "mod")) {
                    function(functionName) {
                        listOf(type) returns type
                    }
                }

                for (functionName in listOf("unaryPlus", "unaryMinus")) {
                    function(functionName) {
                        emptyList<String>() returns type
                    }
                }

                for (functionName in listOf("==", "!=", ">=", "<=", ">", "<")) {
                    function(functionName) {
                        listOf(type) returns "Boolean"
                    }
                }
            }
        }

        "Boolean" {
            for (functionName in listOf("&&", "||")) {
                function(functionName) {
                    listOf("Boolean") returns "Boolean"
                }
            }

            function("not") {
                emptyList<String>() returns "Boolean"
            }
        }
    }
    val typechecker = TypeChecker(env)
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

    // val srcString = """
    //     fun fib(n: Int): Int {
    //         if (n == 0 || n == 1) {
    //             return n;
    //         } else {
    //             return fib(n - 1) + fib(n - 2);
    //         }
    //     }
    //
    //     print("n = ");
    //     val n: Int = readInt();
    //
    //     print("fib(");
    //     print(n);
    //     print(") = ");
    //     println(fib(n));
    // """.trimIndent()

    val srcString = """
        fun quadratic(a: Int, b: Int, c: Int, x: Int): Int {
            var ret: Int = c;

            val q0: Int = a * x * x;
            ret = ret + q0;

            val q1: Int = b * x;
            ret = ret + q1;

            return ret;
        }

        print("a = ");
        val a: Int = readInt();

        print("b = ");
        val b: Int = readInt();

        print("c = ");
        val c: Int = readInt();

        print("up to x = ");
        val x: Int = readInt();

        println("the answers are:");

        var i: Int = 0;
        while (i < x) {
            val tmp: Int = if (i % 2 == 0) {
                val t1: Int = quadratic(a, b, c, i) * 2;
                t1;
            } else {
                val t2: Int = quadratic(a, b, c, i);
                t2;
            };
            print(i);
            print(" -> ");
            println(tmp);
            i = i + 1;
        }
    """.trimIndent()

    // val srcString = """
    //     print("a = ");
    //     val a: Int = readInt();
    //
    //     print("b = ");
    //     val b: Int = readInt();
    //
    //     val toPrint: Int = if (a < b) {
    //         a * 2;
    //     } else {
    //         b * 2;
    //     };
    //
    //     println(toPrint);
    // """.trimIndent()

    // val srcString = """
    //     class Foo constructor(val baz: Int = 10) : Bar {
    //         val qux: Int = this.baz;
    //
    //         constructor(test1: Int, test2: Int = 20) : this(test1 + test2) {
    //             print("secondary constructor with values");
    //             print(test1);
    //             print(" ");
    //             println(test2);
    //         }
    //
    //         fun quux(): Int {
    //             return this.qux + baz;
    //         }
    //
    //         fun corge(): Int {
    //             return quux();
    //         }
    //
    //         fun grault(): Int {
    //             return this.corge() + quux();
    //         }
    //     }
    //
    //     val foo: Foo = Foo();
    //     val ret: Int = foo.grault();
    //     println(ret);
    //
    //     fun id(test: Int = 10): Int {
    //         return test;
    //     }
    //
    //     val a: Int = id(20);
    //
    //     // println(baz);
    //     // println(qux);
    //     // println(quux());
    //     // println(corge());
    //     // println(grault());
    // """.trimIndent()

    val lexer = Lexer(srcString)
    val parser = Parser(lexer.tokens)
    val codegen = CodeGenerator()

    val tree = parser.parse()

    tree.forEach(::println)

    val typedTree = typechecker.check(tree)

    typedTree.forEach(::println)

    val chunk = codegen.generate(typedTree)

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