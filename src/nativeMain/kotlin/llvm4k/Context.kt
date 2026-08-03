package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCValues
import llvm.LLVMContextCreate
import llvm.LLVMContextRef
import llvm.LLVMCreateBuilderInContext
import llvm.LLVMDoubleTypeInContext
import llvm.LLVMInt1TypeInContext
import llvm.LLVMInt32TypeInContext
import llvm.LLVMInt8TypeInContext
import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMOrcCreateNewThreadSafeContextFromLLVMContext
import llvm.LLVMOrcDisposeThreadSafeContext
import llvm.LLVMOrcThreadSafeContextRef
import llvm.LLVMStructTypeInContext
import llvm.LLVMVoidTypeInContext

@OptIn(ExperimentalForeignApi::class)
public class Context internal constructor(private val ref: LLVMContextRef?) {
    public val llvmRef: LLVMContextRef?
        get() = this.ref

    public val int1: Type
        get() = Type(LLVMInt1TypeInContext(this.ref), this)

    public val int8: Type
        get() = Type(LLVMInt8TypeInContext(this.ref), this)

    public val int32: Type
        get() = Type(LLVMInt32TypeInContext(this.ref), this)

    public val double: Type
        get() = Type(LLVMDoubleTypeInContext(this.ref), this)

    public val void: Type
        get() = Type(LLVMVoidTypeInContext(this.ref), this)

    public fun struct(elementTypes: Array<Type>, name: String? = null, packed: Boolean = false): Type {
        return Type(
            LLVMStructTypeInContext(
                this.ref,
                elementTypes.map(Type::llvmRef).toCValues(),
                elementTypes.size.toUInt(),
                if (packed) 1 else 0
            ),
            this
        )
    }

    public fun newBuilder(): Builder {
        return Builder(LLVMCreateBuilderInContext(this.ref))
    }

    public fun newModule(name: String): Module {
        return Module(LLVMModuleCreateWithNameInContext(name, this.ref), this)
    }

    public fun module(name: String, block: Module.() -> Unit): Module {
        return newModule(name).apply(block)
    }

    public companion object {
        public operator fun invoke(): Context {
            return Context(LLVMContextCreate())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
public class ThreadSafeContext private constructor(private val ref: LLVMOrcThreadSafeContextRef?, public val context: Context) {
    private var disposed: Boolean = false

    public val llvmRef: LLVMOrcThreadSafeContextRef?
        get() = this.ref

    public fun dispose() {
        if (!disposed) {
            disposed = true
            LLVMOrcDisposeThreadSafeContext(this.ref)
        }
    }

    public companion object {
        public operator fun invoke(context: Context): ThreadSafeContext {
            return ThreadSafeContext(LLVMOrcCreateNewThreadSafeContextFromLLVMContext(context.llvmRef), context)
        }
    }
}

public fun <R> threadSafeContext(ctx: Context, block: ThreadSafeContext.() -> R): R {
    val tsc = ThreadSafeContext(ctx)

    // update to use(?)
    // or wrap in try {} finally {}
    return tsc.block().also { tsc.dispose() }
}