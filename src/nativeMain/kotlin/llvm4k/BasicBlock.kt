package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMBasicBlockRef

@OptIn(ExperimentalForeignApi::class)
public class BasicBlock internal constructor(private val ref: LLVMBasicBlockRef?) {
    public val llvmRef: LLVMBasicBlockRef?
        get() = this.ref
}