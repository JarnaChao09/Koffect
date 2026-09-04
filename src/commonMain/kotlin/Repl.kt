import analysis.TypeChecker
import analysis.ast.TypedStatement
import analysis.buildEnvironment
import lexer.Lexer
import parser.Parser

public val printTypes: List<String> = listOf(
    "Any",
    "Int",
    "Long",
    "Double",
    "Boolean",
    "String",
    // "Unit",
    // "Nothing?",
)

public fun repl() {
    val env = buildEnvironment {
        function("println") {
            for (type in printTypes) {
                listOf(type) returns "Unit"
            }
            emptyList<String>() returns "Unit"
        }

        function("print") {
            for (type in printTypes) {
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
            emptyList<String>() returns "Long"
        }

        for (type in listOf("Int", "Long", "Double")) {
            type {
                for (functionName in listOf("plus", "minus", "times", "div", "mod")) {
                    function(functionName, operator = true) {
                        listOf(type) returns type
                    }
                }

                for (functionName in listOf("unaryPlus", "unaryMinus")) {
                    function(functionName, /* operator = true */) {
                        emptyList<String>() returns type
                    }
                }

                for (functionName in listOf("==", "!=", ">=", "<=", ">", "<")) {
                    function(functionName, operator = true) {
                        listOf(type) returns "Boolean"
                    }
                }

                for ((functionName, returnType) in listOf("toInt" to "Int", "toLong" to "Long", "toDouble" to "Double")) {
                    function(functionName) {
                        emptyList<String>() returns returnType
                    }
                }
            }
        }

        "String" {
            function("plus", operator = true) {
                listOf("String") returns "String"
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

    // vm.addNativeFunction("clock") {
    //     require(it.isEmpty())
    //
    //     (System.currentTimeMillis() / 1000.0).toValue()
    // }

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

    // val srcString = """
    //     inline fun withInt(intValue1: Int, intValue2: Int, block: context(Int) (Int) -> Unit) {
    //         print("testing withInt with intValues = ");
    //         print(intValue1);
    //         print(" and ");
    //         println(intValue2);
    //         block(intValue1, intValue2);
    //     }
    //
    //     fun foo(bar: Int) {
    //         print("foo with ");
    //         println(bar);
    //     }
    //
    //     fun main() {
    //         val y: Int = 20;
    //         println("testing inlining a function with a trailing lambda");
    //
    //         withInt(10, y + 20) { z: Int ->
    //             foo(this@Int - z);
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     inline fun withInt(intValue: Int, block: context(Int) () -> Unit) {
    //         block(intValue);
    //     }
    //
    //     inline fun withDouble(doubleValue: Double, block: context(Double) () -> Unit) {
    //         block(doubleValue);
    //     }
    //
    //     context(Int) fun foo() {
    //         print("contextual foo with int=");
    //         println(this@Int);
    //     }
    //
    //     context(Double) fun foo() {
    //         print("contextual foo with double=");
    //         println(this@Double);
    //     }
    //
    //     context(Int, Double) fun foo() {
    //         print("contextual foo with both int=");
    //         print(this@Int);
    //         print(" and double=");
    //         println(this@Double);
    //     }
    //
    //     fun main() {
    //         withInt(10) {
    //             foo();
    //         };
    //
    //         withDouble(3.14) {
    //             foo();
    //         };
    //
    //         withInt(42) {
    //             withDouble(31.4) {
    //                 foo();
    //             };
    //         };
    //
    //         withDouble(0.314) {
    //             withInt(37) {
    //                 foo();
    //             };
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun Int.collapse(other: Int): Int {
    //         return this - other + 1;
    //     }
    //
    //     fun main() {
    //         val test: Int = 10.collapse(9);
    //         println(test);
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     inline fun withInt(intValue: Int, block: context(Int) () -> Unit) {
    //         block(intValue);
    //     }
    //
    //     context(Int) fun Int.collapse(other: Int): Int {
    //         print("calling collapse with context=");
    //         print(this@Int);
    //         print(" and receiver=");
    //         println(this);
    //         return this@Int - this + other + 1;
    //     }
    //
    //     fun main() {
    //         val c: Int = 20;
    //         withInt(c) {
    //             val test: Int = 10.collapse(9);
    //             println(test);
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun foo() {
    //         val x: String = "hello";
    //         fun bar() {
    //             println(x);
    //         }
    //
    //         bar();
    //     }
    //
    //     foo();
    // """.trimIndent()

    // val srcString = """
    //     fun foo() {
    //         var x: String = "hello";
    //
    //         fun get() {
    //             println(x);
    //         }
    //
    //         fun set(newValue: String) {
    //             x = newValue;
    //         }
    //
    //         get();
    //         set("world");
    //         get();
    //     }
    //
    //     foo();
    // """.trimIndent()

    // val srcString = """
    //     fun main() {
    //         var x: String = "foo";
    //
    //         val get: () -> Unit = {
    //             println(x);
    //         };
    //
    //         val set: (String) -> Unit = { v: String ->
    //             x = v;
    //         };
    //
    //         get();
    //         set("bar");
    //         get();
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun foo(y: Int): () -> Int {
    //         val x: Int = 100;
    //
    //         return { x + y; };
    //     }
    //
    //     fun main() {
    //         val f: () -> Int = foo(10);
    //         println(f());
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun foo(x: Int): () -> Int {
    //         fun bar(): () -> Int {
    //             return { x; };
    //         }
    //
    //         return bar();
    //     }
    //
    //     fun main() {
    //         val f: () -> Int = foo(1);
    //         println(f());
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun runWith(self: Int, block: Int.() -> Unit) {
    //         self.block();
    //     }
    //
    //     fun main() {
    //         runWith(10) {
    //             println(this * 2);
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     fun with(value: Int, block: context(Int) () -> Unit) {
    //         block(value);
    //     }
    //
    //     fun run(block: () -> Unit) {
    //         block();
    //     }
    //
    //     context(Int) fun foo() {
    //         print("calling foo with ");
    //         println(this@Int);
    //     }
    //
    //     fun main() {
    //         with(10) {
    //             run {
    //                 foo();
    //             };
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     class Foo {}
    //
    //     println(Foo());
    // """.trimIndent()

    // val srcString = """
    //     class Add(val left: Int, val right: Int) {
    //         constructor(combined: Int) : this(combined / 2, combined / 2) {
    //             println(combined);
    //         }
    //     }
    //
    //     fun main() {
    //         val a1: Add = Add(10, 20);
    //         val a2: Add = Add(30);
    //         println(a1.left);
    //         println(a2.right);
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     class Box(var value: Int) {}
    //
    //     fun main() {
    //         val box: Box = Box(42);
    //
    //         println(box.value);
    //
    //         box.value = -42;
    //
    //         println(box.value);
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     class Box(var value: Int) {
    //         fun print(other: Int) {
    //             println(this.value * other);
    //         }
    //     }
    //
    //     fun main() {
    //         val box: Box = Box(42);
    //
    //         box.print(2);
    //
    //         box.value = -42;
    //
    //         box.print(2);
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     class Context(val backing: Int) {
    //         fun contextualFunction() {
    //             println(backing);
    //         }
    //     }
    //
    //     fun withContext(c: Context, block: context(Context) () -> Unit) {
    //         block(c);
    //     }
    //
    //     context(Context)
    //     fun func() {
    //         contextualFunction();
    //     }
    //
    //     fun main() {
    //         withContext(Context(10)) {
    //             func();
    //         };
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     class Context(val backing: Int) {
    //         fun Int.contextualFunction() {
    //             println(backing);
    //         }
    //     }
    //
    //     context(Context)
    //     fun func() {
    //         10.contextualFunction();
    //     }
    //
    //     fun withContext(c: Context, block: context(Context) () -> Unit) {
    //         block(c);
    //     }
    //
    //     fun main() {
    //         withContext(Context(10)) {
    //             func();
    //         };
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun test(foo: Int): Int {
    //         return foo;
    //     }
    //     fun test(foo: Int, bar: Int): Int {
    //         return foo + bar;
    //     }
    //
    //     fun main() {
    //         println(test(5));
    //         println(test(3, 4));
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun even(num: Int): Boolean {
    //         val m: Int = num % 2;
    //         return m == 0;
    //     }
    //
    //     fun isEvenv1(num: Int): Int {
    //         if (even(num)) {
    //             return 100;
    //         } else {
    //             return 50;
    //         }
    //     }
    //
    //     fun isEvenv2(num: Int): Int {
    //         if (even(num)) {
    //            return 100;
    //         }
    //         return 50;
    //     }
    //
    //     fun isEvenv3(num: Int) {
    //         if (even(num)) {
    //             println(100);
    //         } else {
    //             println(50);
    //         }
    //     }
    //
    //     fun isEvenv4(num: Int) {
    //         if (even(num)) {
    //             println(200);
    //         }
    //
    //         println(500);
    //     }
    //
    //     fun main() {
    //         println(isEvenv1(3));
    //         println(isEvenv1(4));
    //
    //         println();
    //
    //         println(isEvenv2(3));
    //         println(isEvenv2(4));
    //
    //         println();
    //
    //         isEvenv3(3);
    //         isEvenv3(4);
    //
    //         println();
    //
    //         isEvenv4(3);
    //         isEvenv4(4);
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun main() {
    //         var a: Int = 0;
    //         var b: Int = 1;
    //
    //         val n: Int = 12;
    //         var i: Int = 0;
    //
    //         while (i < n) {
    //             val tmp: Int = a + b;
    //             a = b;
    //             b = tmp;
    //             i = i + 1;
    //         }
    //
    //         print("fib(");
    //         print(n);
    //         print(")=");
    //         println(a);
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun truth(): Boolean {
    //         println("the truth");
    //         return true;
    //     }
    //
    //     fun lie(): Boolean {
    //         println("the lie");
    //         return false;
    //     }
    //
    //     fun main() {
    //         println(truth() && truth());
    //         println(truth() && lie());
    //         println(lie() && truth());
    //         println(lie() && lie());
    //         println();
    //         println(truth() || truth());
    //         println(truth() || lie());
    //         println(lie() || truth());
    //         println(lie() || lie());
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun run(block: () -> Unit) {
    //         block();
    //     }
    //
    //     fun main() {
    //         val r: () -> Unit = {
    //             println("from lambda literal");
    //         };
    //         run {
    //             println("Hello world");
    //         };
    //         r();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun foo() {
    //         println("contextless foo");
    //     }
    //
    //     context(Int)
    //     fun foo() {
    //         print("context(Int) [");
    //         print(this@Int);
    //         println("] foo");
    //     }
    //
    //     fun withInt(c: Int, block: context(Int) () -> Unit) {
    //         block(c);
    //     }
    //
    //     fun main() {
    //         foo();
    //         withInt(10) {
    //             foo();
    //             foo@();
    //         };
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun fibIter(n: Int): Long {
    //         var a: Long = 0L;
    //         var b: Long = 1L;
    //
    //         var i: Int = 0;
    //
    //         while (i < n) {
    //             val tmp: Long = a + b;
    //             a = b;
    //             b = tmp;
    //             i = i + 1;
    //         }
    //
    //         return a;
    //     }
    //
    //     fun fibRec(n: Int): Long {
    //         if (n == 0) {
    //             return 0L;
    //         } else if (n == 1) {
    //             return 1L;
    //         } else {
    //             return fibRec(n - 1) + fibRec(n - 2);
    //         }
    //     }
    //
    //     fun measureTime(block: () -> Unit): Long {
    //         val t1: Long = clock();
    //         block();
    //         val t2: Long = clock();
    //
    //         return t2 - t1;
    //     }
    //
    //     fun main() {
    //         val iter: Long = measureTime {
    //             val fib: Long = fibIter(30);
    //         };
    //         val recur: Long = measureTime {
    //             val fib: Long = fibRec(30);
    //         };
    //
    //         println(iter);
    //         println(recur);
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Box(val value: Int) {}
    //
    //     fun printBox(box: Box) {
    //         print("Box(");
    //         print(box.value);
    //         println(")");
    //     }
    //
    //     fun mutateBox(box: Box, value: Int) {
    //         box.value = value;
    //     }
    //
    //     fun main() {
    //         val b: Box = Box(5);
    //
    //         printBox(b);
    //         mutateBox(b, 42);
    //         printBox(b);
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Box(var value: Int) {}
    //
    //     fun Box.print() {
    //         print("Box(");
    //         print(this.value);
    //         println(")");
    //     }
    //
    //     fun Box.mutate(value: Int) {
    //         this.value = value;
    //     }
    //
    //     fun main() {
    //         val b: Box = Box(5);
    //
    //         b.print();
    //         b.mutate(42);
    //         b.print();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Add(val left: Int, val right: Int) {
    //         constructor(combined: Int) : this(combined / 2, combined / 2) {
    //             print(this.left);
    //             print(" + ");
    //             print(this.right);
    //             print(" = ");
    //             println(combined);
    //         }
    //     }
    //
    //     fun Add.prettyPrint() {
    //         print(this.left);
    //         print(" + ");
    //         println(this.right);
    //     }
    //
    //     fun main() {
    //         val a1: Add = Add(10, 20);
    //         val a2: Add = Add(30);
    //         a1.prettyPrint();
    //         a2.prettyPrint();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Foo {
    //         var test: Int = -1;
    //     }
    //
    //     fun Foo.print() { print("Foo test="); println(this.test); }
    //
    //     fun main() {
    //         val f: Foo = Foo();
    //
    //         f.print();
    //         f.test = 100;
    //         f.print();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Foo(val a: Int, b: Int, val c: Int) {
    //         var test: Int = -1;
    //     }
    //
    //     fun Foo.print() { print("Foo a="); print(this.a); print(", c="); print(this.c); print(", test="); println(this.test); }
    //
    //     fun main() {
    //         val f: Foo = Foo(3, 4, 5);
    //
    //         f.print();
    //         f.test = 100;
    //         f.print();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Foo(val bar: Int) {
    //         fun baz() {
    //             println(this.bar);
    //         }
    //     }
    //
    //     fun main() {
    //         val foo: Foo = Foo(42);
    //
    //         foo.baz();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun main() {
    //         val c: Boolean = false;
    //         val a: Int = if (c) {
    //             println("true");
    //             100;
    //         } else {
    //             println("false");
    //             200;
    //         };
    //
    //         println(a);
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Context(val backing: Int) {
    //         fun Int.contextualFunction(other: Int) {
    //             println(backing + this + other);
    //         }
    //     }
    //
    //     fun withContext(c: Context, block: context(Context) () -> Unit) {
    //         block(c);
    //     }
    //
    //     context(Context)
    //     fun func() {
    //         9.contextualFunction(8);
    //     }
    //
    //     fun main() {
    //         withContext(Context(10)) {
    //             func();
    //         };
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Num(val num: Int) {
    //         operator fun plus(other: Num): Num {
    //             return Num(this.num + other.num);
    //         }
    //
    //         operator fun minus(other: Num): Num {
    //             return Num(this.num - other.num);
    //         }
    //
    //         fun println() {
    //             print("Num(");
    //             print(num);
    //             println(")");
    //         }
    //     }
    //
    //     fun main() {
    //         val n1: Num = Num(3);
    //         val n2: Num = Num(4);
    //         val n3: Num = Num(5);
    //
    //         (n1 + n2 - n3).println();
    //     }
    // """.trimIndent()

    // val srcString = """
    //     class Num(val num: Int) {
    //         operator fun plus(other: Num): Num {
    //             return Num(this.num + other.num);
    //         }
    //     }
    //     fun println(num: Num) { print("Num("); print(num.num); println(")"); }
    //     class NumContext { operator fun Int.literal(): Num = Num(this); }
    //     fun withContext(block: context(NumContext) () -> Unit) = block(NumContext());
    //
    //     fun main() {
    //         withContext {
    //             println(3 + 4);
    //         };
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun main() {
    //         println("Hello" + " " + "world");
    //     }
    // """.trimIndent()

    // val srcString = """
    //     fun oops(): Int = 42;
    //     class Num(val num: Int) { operator fun plus(other: Num): Num = Num(this.num + other.num); }
    //     fun println(num: Num) { print("Num("); print(num.num); println(")"); }
    //     class NumContext1 {}
    //     class NumContext2 {}
    //     context(NumContext1) operator fun Int.literal(): Num = Num(this);
    //     context(NumContext1) operator fun Double.literal(): Num = Num(oops());
    //     context(NumContext1, NumContext2) operator fun Int.literal(): Num = Num(this + oops());
    //     fun withContext(block: context(NumContext1, NumContext2) () -> Unit) = block(NumContext1(), NumContext2());
    //
    //     fun main() {
    //         withContext { println(42 + 1.0); };
    //     }
    // """.trimIndent()

    val srcString = """
        class Num(val num: Int) { operator fun plus(other: Num): Num = Num(this.num + other.num); }
        fun println(num: Num) { print("Num("); print(num.num); println(")"); }
        class NC1 {}
        class NC2 {}
        context(NC2) operator fun Int.literal(): Int = this * 2@;
        context(NC1, NC2) operator fun Int.literal(): Num = Num(this * 10@ + 10@NC2);
        fun withContext(block: context(NC1, NC2) () -> Unit) = block(NC1(), NC2());
        
        fun main() {
            withContext {
                println(3 + 4);
            };
        }
    """.trimIndent()

    // TODO
    // val srcString = """
    //     class Context1(val backing: Int) {
    //         fun contextualFunction() {
    //             println(backing + 1);
    //         }
    //     }
    //
    //     class Context2(val backing: Int) {
    //         fun contextualFunction() {
    //             println(backing + 2);
    //         }
    //     }
    //
    //     context(Context1, Context2)
    //     fun func() {
    //         contextualFunction();
    //     }
    // """.trimIndent()

    // TODO
    // val srcString = """
    //     class Foo constructor(val bar: Double) {
    //         val baz1: Int = bar.toInt();
    //         val baz2: Int = this.bar.toInt();
    //
    //         constructor(l: Int, r: Int) : this((l + r).toDouble()) {
    //             print("secondary constructor with ");
    //             print(l);
    //             print(" + ");
    //             println(r);
    //         }
    //
    //         fun quux(): Double {
    //             val bar: Double = 3.14;
    //             return bar + this.bar;
    //         }
    //     }
    //
    //     fun main() {
    //         val f1: Foo = Foo(10.0);
    //         val f2: Foo = Foo(3, 4);
    //         // val f3: Foo = Foo(5); // error
    //
    //         println(f1.baz1);
    //         println(f1.baz2);
    //         println(f1.quux());
    //
    //         println(f2.baz1);
    //         println(f2.baz2);
    //         println(f2.quux());
    //     }
    //
    //     main();
    // """.trimIndent()

    // val srcString = """
    //     var get: () -> Unit = {};
    //     var set: (String) -> Unit = { s: String -> println(s); };
    //
    //     fun foo() {
    //         var x: String = "hello";
    //
    //         fun g() {
    //             println(x);
    //         }
    //
    //         fun s(n: String) {
    //             x = n;
    //         }
    //
    //         get = g;
    //         set = s;
    //     }
    //
    //     foo();
    //
    //     get();
    //     set("world");
    //     get();
    // """.trimIndent()

    val lexer = Lexer(srcString)
    val parser = Parser(lexer.tokens)

    val tree = parser.parse()

    tree.forEach(::println)

    val typedTree = typechecker.check(tree)

    typedTree.forEach(::println)

    execute(typedTree)

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

public expect fun execute(typedTree: List<TypedStatement>)