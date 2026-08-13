package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMAddFunction

@OptIn(ExperimentalForeignApi::class)
public class FunctionCollection internal constructor(private val mod: Module) {
    private val functions: MutableMap<String, Pair<Type, Function>> = mutableMapOf()

    public fun add(name: String, parameterTypes: List<Type>, returnType: Type, vararg: Boolean = false): Pair<Type, Function> {
        val functionType = Type.Function(this.mod.context, parameterTypes, returnType, vararg)

        val function = LLVMAddFunction(this.mod.llvmRef, name, functionType.llvmRef)

        return (functionType to Function(function)).also {
            this.functions[name] = it
        }
    }

    public fun add(name: String, functionType: Type): Pair<Type, Function> {
        val function = LLVMAddFunction(this.mod.llvmRef, name, functionType.llvmRef)

        return (functionType to Function(function)).also {
            this.functions[name] = it
        }
    }

    public operator fun get(name: String): Pair<Type, Function> {
        return this.functions[name]!!
    }
}