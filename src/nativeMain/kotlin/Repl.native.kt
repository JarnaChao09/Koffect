import analysis.ast.TypedStatement
import codegen.CodeGenerator
import runtime.UnitValue
import runtime.VM
import runtime.toValue
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.pow

public actual fun execute(typedTree: List<TypedStatement>) {
    val codegen = CodeGenerator()
    val vm = VM()

    for (inputType in printTypes) {
        vm.addNativeFunction("println//$inputType/Unit") {
            // require(it.size == 1)
            println(it[0])
            UnitValue
        }

        vm.addNativeFunction("print//$inputType/Unit") {
            // require(it.size == 1)
            print(it[0])
            UnitValue
        }
    }

    vm.addNativeFunction("println///Unit") {
        println()
        UnitValue
    }

    vm.addNativeFunction("pow") {
        // require(it.size == 2)
        val (a, b) = it
        require(a.value is Double)
        require(b.value is Double)

        val av = a.value as Double
        val bv = b.value as Double

        av.pow(bv).toValue()
    }

    vm.addNativeFunction("readInt") {
        // require(it.isEmpty())

        readln().toInt().toValue()
    }

    vm.addNativeFunction("readDouble") {
        // require(it.isEmpty())

        readln().toDouble().toValue()
    }

    val chunk = codegen.generate(typedTree)

    vm.interpret(chunk)
}