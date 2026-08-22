package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCValues
import llvm.LLVMConstInt
import llvm.LLVMConstNull
import llvm.LLVMConstReal
import llvm.LLVMFunctionType
import llvm.LLVMPointerTypeInContext
import llvm.LLVMTypeRef

@OptIn(ExperimentalForeignApi::class)
public class Type internal constructor(private val ref: LLVMTypeRef?, private val context: Context) {
    public val llvmRef: LLVMTypeRef?
        get() = this.ref

    public fun constInt(value: Int): Value {
        return LLVMConstInt(this.ref, value.toULong(), 0)
    }

    public fun constInt(value: UInt): Value {
        return LLVMConstInt(this.ref, value.toULong(), 0)
    }

    public fun constInt(value: Long): Value {
        return LLVMConstInt(this.ref, value.toULong(), 0)
    }

    public fun constInt(value: ULong): Value {
        return LLVMConstInt(this.ref, value, 0)
    }

    public fun constDouble(value: Double): Value {
        return LLVMConstReal(this.ref, value)
    }

    public fun constNull(): Value {
        return LLVMConstNull(this.ref)
    }

    public val pointer: Type
        get() = Type(LLVMPointerTypeInContext(context.llvmRef, 0U), context)

    public companion object {
        public fun Function(context: Context, parameterTypes: List<Type>, returnType: Type, vararg: Boolean = false): Type {
            val llvmFunctionType = LLVMFunctionType(
                ReturnType = returnType.llvmRef,
                ParamTypes = parameterTypes.map(Type::llvmRef).toCValues(),
                ParamCount = parameterTypes.size.toUInt(),
                IsVarArg = if (vararg) 1 else 0,
            )

            return Type(llvmFunctionType, context)
        }
    }
}