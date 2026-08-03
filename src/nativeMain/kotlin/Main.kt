import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import llvm.LLVMGetVersion
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
public fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Flags currently unsupported")
        exitProcess(64)
    } else if (args.size == 1) {
        println("Running file currently unsupported")
        exitProcess(64)
    } else {
        repl()
        // memScoped {
        //     val major = alloc<UIntVar>()
        //     val minor = alloc<UIntVar>()
        //     val patch = alloc<UIntVar>()
        //
        //     LLVMGetVersion(major.ptr, minor.ptr, patch.ptr)
        //
        //     println("LLVM Version ${major.value}.${minor.value}.${patch.value}")
        // }
    }
}