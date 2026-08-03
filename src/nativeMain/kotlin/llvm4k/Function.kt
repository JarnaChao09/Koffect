package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMCountParams
import llvm.LLVMGetParam

@OptIn(ExperimentalForeignApi::class)
public class Function internal constructor(private val ref: Value) {
    public val llvmRef: Value
        get() = this.ref

    public val basicBlocks: BasicBlockCollection
        get() = BasicBlockCollection(this)

    private fun getParam(index: UInt): Value {
        return LLVMGetParam(this.ref, index)
    }

    public val parameters: List<Value> = List(LLVMCountParams(this.ref).toInt()) {
        getParam(it.toUInt())
    }
}