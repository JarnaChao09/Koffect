package llvm4k

import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMAppendBasicBlockInContext
import llvm.LLVMGetGlobalParent
import llvm.LLVMGetModuleContext

@OptIn(ExperimentalForeignApi::class)
public class BasicBlockCollection internal constructor(private val function: Function) {
    public fun append(name: String = "", block: Builder.(Function) -> Unit): BasicBlock {
        val context = LLVMGetModuleContext(LLVMGetGlobalParent(this.function.llvmRef))
        val ret = BasicBlock(LLVMAppendBasicBlockInContext(context, this.function.llvmRef, name))

        val builder = Builder(context)
        builder.positionAtEnd(ret)

        // update to use(?)
        // or wrap in try {} finally {}
        builder.block(this.function)

        builder.dispose()

        return ret
    }
}