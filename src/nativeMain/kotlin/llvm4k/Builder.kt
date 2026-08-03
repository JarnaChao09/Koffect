package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCValues
import llvm.LLVMBuildAdd
import llvm.LLVMBuildAlloca
import llvm.LLVMBuildCall2
import llvm.LLVMBuildExactSDiv
import llvm.LLVMBuildExactUDiv
import llvm.LLVMBuildExtractValue
import llvm.LLVMBuildFCmp
import llvm.LLVMBuildFDiv
import llvm.LLVMBuildFMul
import llvm.LLVMBuildFRem
import llvm.LLVMBuildGEP2
import llvm.LLVMBuildGlobalStringPtr
import llvm.LLVMBuildICmp
import llvm.LLVMBuildLoad2
import llvm.LLVMBuildMul
import llvm.LLVMBuildRet
import llvm.LLVMBuildSDiv
import llvm.LLVMBuildSRem
import llvm.LLVMBuildSelect
import llvm.LLVMBuildStore
import llvm.LLVMBuildSub
import llvm.LLVMBuildUDiv
import llvm.LLVMBuildURem
import llvm.LLVMBuilderRef
import llvm.LLVMContextRef
import llvm.LLVMCreateBuilderInContext
import llvm.LLVMDisposeBuilder
import llvm.LLVMIntPredicate
import llvm.LLVMPositionBuilderAtEnd
import llvm.LLVMRealPredicate

@OptIn(ExperimentalForeignApi::class)
public class Builder internal constructor(private val ref: LLVMBuilderRef?) {
    private var disposed: Boolean = false

    public fun positionAtEnd(block: BasicBlock) {
        LLVMPositionBuilderAtEnd(this.ref, block.llvmRef)
    }

    public fun alloca(type: Type, name: String = ""): Value {
        return LLVMBuildAlloca(this.ref, type.llvmRef, name)
    }

    public fun load(type: Type, pointerValue: Value, name: String = ""): Value {
        return LLVMBuildLoad2(this.ref, type.llvmRef, pointerValue, name)
    }

    public fun store(value: Value, pointer: Value): Value {
        return LLVMBuildStore(this.ref, value, pointer)
    }

    public fun add(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildAdd(this.ref, left, right, name)
    }

    public fun sub(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildSub(this.ref, left, right, name)
    }

    public fun mul(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildMul(this.ref, left, right, name)
    }

    public fun fmul(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildFMul(this.ref, left, right, name)
    }

    public fun udiv(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildUDiv(this.ref, left, right, name)
    }

    public fun udivExact(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildExactUDiv(this.ref, left, right, name)
    }

    public fun sdiv(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildSDiv(this.ref, left, right, name)
    }

    public fun sdivExact(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildExactSDiv(this.ref, left, right, name)
    }

    public fun fdiv(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildFDiv(this.ref, left, right, name)
    }

    public fun urem(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildURem(this.ref, left, right, name)
    }

    public fun srem(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildSRem(this.ref, left, right, name)
    }

    public fun frem(left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildFRem(this.ref, left, right, name)
    }

    public fun icmp(predicate: LLVMIntPredicate, left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildICmp(this.ref, predicate, left, right, name)
    }

    public fun fcmp(predicate: LLVMRealPredicate, left: Value, right: Value, name: String = ""): Value {
        return LLVMBuildFCmp(this.ref, predicate, left, right, name)
    }

    public fun call(functionType: Type, function: Value, args: Array<Value>, name: String = ""): Value {
        return LLVMBuildCall2(
            this.ref,
            functionType.llvmRef,
            function,
            args.toCValues(),
            args.size.toUInt(),
            name
        )
    }

    public fun select(cond: Value, thenValue: Value, elseValue: Value, name: String = ""): Value {
        return LLVMBuildSelect(this.ref, cond, thenValue, elseValue, name)
    }

    public fun globalStringPointer(str: String, name: String = ""): Value {
        return LLVMBuildGlobalStringPtr(this.ref, str, name)
    }

    public fun extractValue(value: Value, index: UInt, name: String = ""): Value {
        return LLVMBuildExtractValue(this.ref, value, index, name)
    }

    public fun gep(type: Type, pointer: Value, indices: Array<Value>, name: String = ""): Value {
        return LLVMBuildGEP2(this.ref, type.llvmRef, pointer, indices.toCValues(), indices.size.toUInt(), name)
    }

    public fun ret(): Value {
        return LLVMBuildRet(this.ref, null)
    }

    public fun ret(value: Value): Value {
        return LLVMBuildRet(this.ref, value)
    }

    public fun dispose() {
        if (!disposed) {
            disposed = true
            LLVMDisposeBuilder(this.ref)
        }
    }

    public companion object {
        public operator fun invoke(context: LLVMContextRef?): Builder {
            return Builder(LLVMCreateBuilderInContext(context))
        }
    }
}