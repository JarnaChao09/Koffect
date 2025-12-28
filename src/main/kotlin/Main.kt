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

        function("clock") {
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

                for ((functionName, returnType) in listOf("toInt" to "Int", "toDouble" to "Double")) {
                    function(functionName) {
                        emptyList<String>() returns returnType
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

    for (inputType in listOf("Int", "Double", "Boolean", "String", "Unit", "Nothing?")) {
        vm.addNativeFunction("println//$inputType/Unit") {
            assert(it.size == 1)
            println(it[0])
            UnitValue
        }

        vm.addNativeFunction("print//$inputType/Unit") {
            assert(it.size == 1)
            print(it[0])
            UnitValue
        }
    }

    vm.addNativeFunction("println///Unit") {
        println()
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

    vm.addNativeFunction("clock") {
        assert(it.isEmpty())

        (System.currentTimeMillis() / 1000.0).toValue()
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

    // val srcString = """
    //     fun fib(n: Int): Int {
    //         if (n == 0 || n == 1) {
    //             return n;
    //         } else {
    //             return fib(n - 1) + fib(n - 2);
    //         }
    //     }
    //
    //     val before: Double = clock();
    //     println(fib(20));
    //     val after: Double = clock();
    //     println(after - before);
    // """.trimIndent()

    // val srcString = """
    //     fun quadratic(a: Int, b: Int, c: Int, x: Int): Int {
    //         var ret: Int = c;
    //
    //         val q0: Int = a * x * x;
    //         ret = ret + q0;
    //
    //         val q1: Int = b * x;
    //         ret = ret + q1;
    //
    //         return ret;
    //     }
    //
    //     print("a = ");
    //     val a: Int = readInt();
    //
    //     print("b = ");
    //     val b: Int = readInt();
    //
    //     print("c = ");
    //     val c: Int = readInt();
    //
    //     print("up to x = ");
    //     val x: Int = readInt();
    //
    //     println("the answers are:");
    //
    //     var i: Int = 0;
    //     while (i < x) {
    //         val tmp: Int = if (i % 2 == 0) {
    //             val t1: Int = quadratic(a, b, c, i) * 2;
    //             t1;
    //         } else {
    //             val t2: Int = quadratic(a, b, c, i);
    //             t2;
    //         };
    //         print(i);
    //         print(" -> ");
    //         println(tmp);
    //         i = i + 1;
    //     }
    // """.trimIndent()

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
    //     class Foo constructor(val baz: Double = 10.0) : Bar {
    //         val qux: Int = this.baz.toInt();
    //
    //         constructor(test1: Int, test2: Int = 20) : this((test1 + test2).toDouble()) {
    //             print("secondary constructor with values");
    //             print(test1);
    //             print(" ");
    //             println(test2);
    //         }
    //
    //         fun quux(): Int {
    //             val baz: Int = 10;
    //             return this.qux + baz + this.baz.toInt();
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

    // val srcString = """
    //     fun test0(lambda: () -> Unit) {
    //         // val l0: () -> Unit = {
    //         //     println("Hello");
    //         // };
    //         // val l1: context(Int) () -> Unit = { context(Int) ->
    //         //     println("Hello");
    //         // };
    //         // val l2: (Int, Int) -> Int = { x: Int, y: Int -> x + y; };
    //         // val l3: context(Int) (Int) -> Int = { context(Int) x: Int -> x; };
    //         lambda();
    //     }
    //
    //     // fun test1(lambda: (Int) -> Int) {
    //     //     lambda(1);
    //     // }
    //
    //     // fun test2(lambda: (Int, Int) -> Int) {
    //     //     lambda(1, 2);
    //     // }
    //
    //     // fun test3(lambda: ((Int, Int) -> Int)) {
    //     //     lambda(1,2);
    //     // }
    //
    //     context(Int) fun test6() {
    //     }
    //
    //     fun test4(lambda: context(Int) (Int) -> Int) {
    //         lambda(3, 4);
    //         // test6();
    //     }
    //
    //     context(Int) fun test5(lambda: context(Double, Int) (Int) -> Int) {
    //         lambda(3.0, 5);
    //         test6();
    //     }
    //
    //     fun main() {
    //         // test0() {
    //         //     println("Hello");
    //         // };
    //         //
    //         // test0 {
    //         //     println("Hello");
    //         // };
    //
    //         test4 { context(Int) z: Int ->
    //             test5 { context(Double, Int) x: Int ->
    //                 println(this@Double);
    //                 println(x);
    //                 x;
    //             };
    //             println(this@Int);
    //             println(z);
    //             z;
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     context(Int) fun foo() {
    //         print("contextual int foo with ");
    //         println(this@Int);
    //     }
    //
    //     fun foo() {
    //         println("foo");
    //     }
    //
    //     context(Double) fun foo() {
    //         print("contextual double foo with ");
    //         println(this@Double);
    //     }
    //
    //     context(Int, Double) fun foo() {
    //         print("contextual int and double foo with ");
    //         print(this@Int);
    //         print(" ");
    //         println(this@Double);
    //     }
    //
    //     fun withInt(value: Int, block: context(Int) () -> Unit) {
    //         block(value);
    //     }
    //
    //     fun withDouble(value: Double, block: context(Double) () -> Unit) {
    //         block(value);
    //     }
    //
    //     fun withIntAndDouble(intValue: Int, doubleValue: Double, block: context(Int, Double) () -> Unit) {
    //         block(intValue, doubleValue, block);
    //     }
    //
    //     fun main() {
    //         foo();
    //
    //         // withInt(10) {
    //         //     print("current context value is ");
    //         //     println(this@Int);
    //         //     foo();
    //         // };
    //
    //         // withDouble(10.0) {
    //         //     print("current context value is ");
    //         //     println(this@Double);
    //         //     foo();
    //         // };
    //
    //         // currently does not work as capture semantics are not implemented in the code generator
    //         // withInt(1) {
    //         //     withDouble(1.0) {
    //         //         foo@Int();    // specifically calling to context(Int)         foo
    //         //         foo@Double(); // specifically calling to context(Double)      foo
    //         //         foo();        // specifically calling to context(Int, Double) foo
    //         //     };
    //         // };
    //
    //         // to work around this, linearize the context introduction function
    //         withIntAndDouble(1, 1.0) {
    //             foo@Int();        // specifically calling to context(Int)         foo
    //             foo@Double();     // specifically calling to context(Double)      foo
    //             foo@();           // specifically calling to context()            foo // should be this allowed?
    //             foo();            // specifically calling to context(Int, Double) foo
    //             foo@Double,Int(); // specifically calling to context(Int, Double) foo
    //         };
    //
    //         foo();
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     context(Int) fun foo() = delete("sorry, deleted");
    //
    //     fun foo() {
    //         println("foo");
    //     }
    //
    //     context(Double) fun foo() = delete;
    //
    //     context(Int, Double) fun foo() {
    //         print("contextual int and double foo with ");
    //         print(this@Int);
    //         print(" ");
    //         println(this@Double);
    //     }
    //
    //     fun withIntAndDouble(intValue: Int, doubleValue: Double, block: context(Int, Double) () -> Unit) {
    //         block(intValue, doubleValue, block);
    //     }
    //
    //     fun main() {
    //         foo();
    //
    //         withIntAndDouble(1, 1.0) {
    //             // foo@Int();        // specifically calling to context(Int)         foo // which is deleted
    //             // foo@Double();     // specifically calling to context(Double)      foo // which is deleted
    //             foo@();           // specifically calling to context()            foo // should be this allowed?
    //             foo();            // specifically calling to context(Int, Double) foo
    //             foo@Double,Int(); // specifically calling to context(Int, Double) foo
    //         };
    //
    //         foo();
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     inline fun foo(bar: Int) {
    //         val baz: Int = bar + bar;
    //
    //         if (baz % 2 == 0) {
    //             println("was even");
    //             return;
    //         }
    //
    //         println(baz);
    //     }
    //
    //     fun main() {
    //         val uniqueName: Int = 30;
    //         val bar: Int = 100;
    //         val baz: Int = -1;
    //         val unused: Unit = foo(uniqueName * uniqueName);
    //
    //         print("bar = ");
    //         println(bar);
    //         print("baz = ");
    //         println(baz);
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     inline fun withInt(intValue: Int, block: context(Int) () -> Unit) {
    //         print("testing withInt with intValue = ");
    //         println(intValue);
    //         block(intValue);
    //     }
    //
    //     context(Int) fun foo() {
    //         print("contextual int foo with ");
    //         println(this@Int);
    //     }
    //
    //     fun main() {
    //         println("testing inlining a function with a trailing lambda");
    //
    //         withInt(10) {
    //             foo();
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    val srcString = """
        inline fun withInt(intValue: Int, block: (Int) -> Unit) {
            print("testing withInt with intValue = ");
            println(intValue);
            block(intValue);
        }
        
        fun foo(bar: Int) {
            print("foo with ");
            println(bar);
        }
        
        fun main() {
            val y: Int = 20;
            println("testing inlining a function with a trailing lambda");
            
            withInt(10) { x: Int ->
                foo(x + y);
            };
        }
        
        main();
    """.trimIndent()

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