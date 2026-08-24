package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMAddGlobal
import llvm.LLVMGetNamedGlobal

@OptIn(ExperimentalForeignApi::class)
public class GlobalCollection internal constructor(private val mod: Module) {
    public fun add(name: String, type: Type): Value {
        val global = LLVMAddGlobal(mod.llvmRef, type.llvmRef, name)

        return global
    }

    public operator fun get(name: String): Value {
        val global = LLVMGetNamedGlobal(mod.llvmRef, name)

        return global ?: error("Global not found: $name")
    }
}