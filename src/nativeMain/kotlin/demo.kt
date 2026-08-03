import kotlinx.cinterop.ExperimentalForeignApi
import llvm.LLVMDumpModule
import llvm4k.Context
import llvm4k.Context.Companion.invoke
import llvm4k.ThreadSafeModule
import llvm4k.ThreadSafeModule.Companion.invoke
import llvm4k.threadSafeContext

@OptIn(ExperimentalForeignApi::class)
private fun createDemoModule(): ThreadSafeModule = threadSafeContext(Context()) {
    val module = context.module("demo") {
        val structType = context.struct(
            arrayOf(
                context.int32, // x
                context.int32, // y
                context.int32, // sum
                context.int32, // prod
            ),
            name = "SumProd",
        )
        function("printf", listOf(context.int8.pointer), context.int32, vararg = true)

        function("sum_and_prod", listOf(context.int32, context.int32), structType) {
            basicBlocks.append("entry") {
                val firstArg = it.parameters[0]
                val secondArg = it.parameters[1]

                val ret = alloca(structType)

                val x = gep(structType, ret, arrayOf(context.int8.constInt(0), context.int8.constInt(0)))

                store(firstArg, x)

                val y = gep(structType, ret, arrayOf(context.int8.constInt(0), context.int8.constInt(1)))

                store(secondArg, y)

                val addResult = add(firstArg, secondArg)

                val addField = gep(structType, ret, arrayOf(context.int8.constInt(0), context.int8.constInt(2)))

                store(addResult, addField)

                val prodResult = mul(firstArg, secondArg)

                val prodField = gep(structType, ret, arrayOf(context.int8.constInt(0), context.int8.constInt(3)))

                store(prodResult, prodField)

                val retValue = load(structType, ret)

                ret(retValue)
            }
        }

        function("main", listOf(context.int32, context.int32), context.int32) {
            basicBlocks.append("entry") {
                val arg1 = it.parameters[0]
                val arg2 = it.parameters[1]

                val (sumFunctionType, sumFunction) = functions["sum_and_prod"]
                val callResult = call(sumFunctionType, sumFunction.llvmRef, arrayOf(arg1, arg2))

                val (printFunctionType, printFunction) = functions["printf"]
                val printfString = globalStringPointer("%d + %d = %d\n%d * %d = %d\n")

                val result = alloca(structType)

                val callResultX = extractValue(callResult, 0u)
                val callResultY = extractValue(callResult, 1u)
                val callResultSum = extractValue(callResult, 2u)
                val callResultProd = extractValue(callResult, 3u)

                val resultX = gep(structType, result, arrayOf(context.int8.constInt(0), context.int8.constInt(0)))
                val resultY = gep(structType, result, arrayOf(context.int8.constInt(0), context.int8.constInt(1)))
                val resultSum = gep(structType, result, arrayOf(context.int8.constInt(0), context.int8.constInt(2)))
                val resultProd = gep(structType, result, arrayOf(context.int8.constInt(0), context.int8.constInt(3)))

                store(callResultX, resultX)
                store(callResultY, resultY)
                store(callResultSum, resultSum)
                store(callResultProd, resultProd)

                val x = load(context.int32, resultX)
                val y = load(context.int32, resultY)
                val sum = load(context.int32, resultSum)
                val prod = load(context.int32, resultProd)

                call(printFunctionType, printFunction.llvmRef, arrayOf(
                    printfString, x, y, sum, x, y, prod,
                ))

                ret(context.int32.constInt(0))
            }
        }
    }

    LLVMDumpModule(module.llvmRef)

    val threadSafeModule = ThreadSafeModule(module, this)

    threadSafeModule
}