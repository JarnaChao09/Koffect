import analysis.ast.TypedStatement
import codegen.LLVMCodeGenerator
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMDisposeErrorMessage
import llvm.LLVMDisposeMessage
import llvm.LLVMErrorRef
import llvm.LLVMErrorRefVar
import llvm.LLVMGetErrorMessage
import llvm.LLVMOrcCreateLLJIT
import llvm.LLVMOrcDisposeLLJIT
import llvm.LLVMOrcDisposeThreadSafeModule
import llvm.LLVMOrcJITTargetAddressVar
import llvm.LLVMOrcLLJITAddLLVMIRModule
import llvm.LLVMOrcLLJITGetMainJITDylib
import llvm.LLVMOrcLLJITLookup
import llvm.LLVMOrcLLJITRefVar
import llvm.LLVMPrintModuleToString
import llvm4k.Function
import llvm4k.ThreadSafeModule


@OptIn(ExperimentalForeignApi::class)
public actual fun execute(typedTree: List<TypedStatement>) {
    try {
        val llvm = LLVMCodeGenerator("koffect main")

        fun String.toLLVMType() = when (this) {
            "Int" -> llvm.type { int32 }
            "Double" -> llvm.type { double }
            "Boolean" -> llvm.type { int1 }
            "String" -> llvm.type { int8.pointer }
            "Any" -> llvm.type { void.pointer }
            "Unit" -> llvm.type { void } // note: void for now
            else -> llvm.type { void.pointer }
        }

        llvm.nativeFunction(
            name = "printf",
            parameterTypes = listOf(llvm.type { int8.pointer }),
            returnType = llvm.type { int32 },
            vararg = true
        )

        context(function: Function)
        fun String.generatePrint(newline: Boolean) {
            val newline = if (newline) "\n" else ""

            function.basicBlocks.append {
                val input = it.parameters[0]

                val (printfFunctionType, printfFunction) = llvm.getNativeFunction("printf")
                    ?: error("printf was not found (???)")

                val args = when (this@generatePrint) {
                    "Int" -> {
                        val output = globalStringPointer("%d$newline")

                        arrayOf(output, input)
                    }

                    "Double" -> {
                        val output = globalStringPointer("%.16f$newline")

                        arrayOf(output, input)
                    }

                    "Boolean" -> {
                        val output =
                            select(input, globalStringPointer("true$newline"), globalStringPointer("false$newline"))

                        arrayOf(output)
                    }

                    "String" -> {
                        val output = globalStringPointer("%s$newline")

                        arrayOf(output, input)
                    }

                    "Any" -> {
                        val output = globalStringPointer("%p$newline")

                        arrayOf(output, input)
                    }

                    else -> error("unsupported type (${this@generatePrint}) to format print")
                }

                call(printfFunctionType, printfFunction.llvmRef, args)

                ret()
            }
        }

        for (inputType in printTypes) {
            llvm.nativeFunction(
                "println",
                parameterTypes = listOf(inputType.toLLVMType() to inputType),
                returnType = "Unit".let { it.toLLVMType() to it },
            ) {
                inputType.generatePrint(true)
            }

            llvm.nativeFunction(
                "print",
                parameterTypes = listOf(inputType.toLLVMType() to inputType),
                returnType = "Unit".let { it.toLLVMType() to it },
            ) {
                inputType.generatePrint(false)
            }
        }

        val module = llvm.generate(typedTree)

        val output = LLVMPrintModuleToString(module.module.llvmRef)

        println(output?.toKString())

        LLVMDisposeMessage(output)

        runOrcJITForModule(module)
    } catch (_: LLVMShutdown) {
    } finally {
        LLVMShutdown()
    }
}

private class LLVMShutdown : Exception()

private class JITCleanUp : Exception()

@OptIn(ExperimentalForeignApi::class)
private fun handleLLVMError(error: LLVMErrorRef?) {
    val llvmErrorMessage = LLVMGetErrorMessage(error)
    println("Error: ${llvmErrorMessage?.toKString() ?: "No Error String given"}")
    LLVMDisposeErrorMessage(llvmErrorMessage)
}

@OptIn(ExperimentalForeignApi::class)
private fun runOrcJITForModule(module: ThreadSafeModule) {
    memScoped {
        val jit = alloc<LLVMOrcLLJITRefVar>()

        val error = alloc<LLVMErrorRefVar>()

        error.value = LLVMOrcCreateLLJIT(jit.ptr, null)

        // println("created LL JIT")

        if (error.value != null) {
            handleLLVMError(error.value)

            throw LLVMShutdown()
        }

        try {
            val jitdylib = LLVMOrcLLJITGetMainJITDylib(jit.value)!!

            // println("retrieved JIT Dylib")

            val error2 = alloc<LLVMErrorRefVar>()

            error2.value = LLVMOrcLLJITAddLLVMIRModule(jit.value, jitdylib, module.llvmRef)

            // println("added LLVM IR Module")

            if (error2.value != null) {
                LLVMOrcDisposeThreadSafeModule(module.llvmRef)
                handleLLVMError(error2.value)

                throw JITCleanUp()
            }

            val mainAddress = alloc<LLVMOrcJITTargetAddressVar>()

            val error3 = alloc<LLVMErrorRefVar>()

            error3.value = LLVMOrcLLJITLookup(jit.value, mainAddress.ptr, "main")

            // println("looked up address of main function")

            if (error3.value != null) {
                handleLLVMError(error3.value)

                throw JITCleanUp()
            }

            // println("main address found at ${mainAddress.value}")

            val mainFunc = mainAddress.value.toLong().toCPointer<CFunction<() -> Unit>>()!!

            // println("reinterpreted main function")

            mainFunc.invoke()

            // println("main exited successfully")
        } catch (_: JITCleanUp) {
        } finally {
            val cleanUpError = alloc<LLVMErrorRefVar>()

            cleanUpError.value = LLVMOrcDisposeLLJIT(jit.value)

            if (cleanUpError.value != null) {
                handleLLVMError(cleanUpError.value)
            }
        }
    }
}