package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMModuleRef
import llvm.LLVMOrcCreateNewThreadSafeModule
import llvm.LLVMOrcThreadSafeModuleRef

@OptIn(ExperimentalForeignApi::class)
public class Module internal constructor(private val ref: LLVMModuleRef?, public val context: Context) {
    public val llvmRef: LLVMModuleRef?
        get() = this.ref

    public val functions: FunctionCollection by lazy { FunctionCollection(this) }

    public fun function(
        name: String,
        parameterTypes: List<Type>,
        returnType: Type,
        vararg: Boolean = false,
        block: Function.(Type) -> Unit = {}
    ): Pair<Type, Function> {
        return this.functions.add(name, parameterTypes, returnType, vararg).apply {
            second.block(first)
        }
    }

    public fun function(
        name: String,
        functionType: Type,
        block: Function.(Type) -> Unit = {}
    ): Pair<Type, Function> {
        return this.functions.add(name, functionType).apply {
            second.block(first)
        }
    }

    public companion object
}

@OptIn(ExperimentalForeignApi::class)
public class ThreadSafeModule internal constructor(private val ref: LLVMOrcThreadSafeModuleRef?, public val module: Module) {
    public val llvmRef: LLVMOrcThreadSafeModuleRef?
        get() = this.ref

    public companion object {
        public operator fun invoke(module: Module, threadSafeContext: ThreadSafeContext): ThreadSafeModule {
            return ThreadSafeModule(LLVMOrcCreateNewThreadSafeModule(module.llvmRef, threadSafeContext.llvmRef), module)
        }
    }
}