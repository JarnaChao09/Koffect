package codegen

import analysis.ast.ClassType
import analysis.ast.FunctionType
import analysis.ast.LambdaType
import analysis.ast.TypedAssign
import analysis.ast.TypedBinary
import analysis.ast.TypedBooleanLiteral
import analysis.ast.TypedCall
import analysis.ast.TypedClassDeclaration
import analysis.ast.TypedContextVariable
import analysis.ast.TypedDeleteStatement
import analysis.ast.TypedDoubleLiteral
import analysis.ast.TypedExpression
import analysis.ast.TypedExpressionStatement
import analysis.ast.TypedFunctionDeclaration
import analysis.ast.TypedGet
import analysis.ast.TypedGrouping
import analysis.ast.TypedIfExpression
import analysis.ast.TypedIfStatement
import analysis.ast.TypedInlineCall
import analysis.ast.TypedIntLiteral
import analysis.ast.TypedLambda
import analysis.ast.TypedLogical
import analysis.ast.TypedLongLiteral
import analysis.ast.TypedNullLiteral
import analysis.ast.TypedReturnExpressionStatement
import analysis.ast.TypedReturnStatement
import analysis.ast.TypedSet
import analysis.ast.TypedStatement
import analysis.ast.TypedStringLiteral
import analysis.ast.TypedThis
import analysis.ast.TypedUnary
import analysis.ast.TypedVariable
import analysis.ast.TypedVariableStatement
import analysis.ast.TypedWhileStatement
import analysis.ast.VariableType
import kotlinx.cinterop.ExperimentalForeignApi
import lexer.TokenType
import llvm.LLVMInitializeNativeAsmPrinter
import llvm.LLVMInitializeNativeTarget
import llvm.LLVMIntPredicate
import llvm.LLVMRealPredicate
import llvm4k.BasicBlock
import llvm4k.Builder
import llvm4k.Context
import llvm4k.Function
import llvm4k.Module
import llvm4k.ThreadSafeContext
import llvm4k.ThreadSafeModule
import llvm4k.Type
import llvm4k.Value

@OptIn(ExperimentalForeignApi::class)
public class LLVMCodeGenerator(moduleName: String) {
    init {
        LLVMInitializeNativeTarget()
        LLVMInitializeNativeAsmPrinter()
    }

    private val threadSafeContext: ThreadSafeContext = ThreadSafeContext(Context())
    private val context: Context = threadSafeContext.context
    private val module: Module = threadSafeContext.context.newModule(moduleName)

    private var function: Function? = null
    private var functionName: String? = null

    private var env: LLVMEnvironment = LLVMEnvironment(null)

    private var returnEmitted: Boolean = false

    public fun nativeFunction(
        name: String,
        parameterTypes: List<Type>,
        returnType: Type,
        vararg: Boolean = false,
        block: Function.(Type) -> Unit = {}
    ) {
        val function = this.module.function(
            name,
            parameterTypes,
            returnType,
            vararg,
            block
        )
        env.addFunction(name, function)
    }

    public fun nativeFunction(
        name: String,
        parameterTypes: List<Pair<Type, String>>,
        returnType: Pair<Type, String>,
        vararg: Boolean = false,
        mangledName: String = generateMangledName(name, parameterTypes.map(Pair<*, String>::second), returnType.second),
        block: Function.(Type) -> Unit = {}
    ) {
        val function = this.module.function(
            name,
            parameterTypes.map(Pair<Type, *>::first),
            returnType.first,
            vararg,
            block
        )
        env.addFunction(mangledName, function)
    }

    public fun generate(ast: List<TypedStatement>): ThreadSafeModule {
        generateStatements(ast, context.newBuilder(), BasicBlock(null))

        return ThreadSafeModule(this.module, this.threadSafeContext)
    }

    private fun generateStatements(ast: List<TypedStatement>, builder: Builder, block: BasicBlock): BasicBlock {
        var currentBlock = block

        ast.forEach {
            builder.positionAtEnd(currentBlock)
            currentBlock = when (it) {
                is TypedClassDeclaration -> {
                    val elementTypes =
                        it.primaryConstructor?.let { primary ->
                            primary.parameters.filterIndexed { i, _ ->
                                primary.parameterTypes[i] != TypedClassDeclaration.FieldType.NONE
                            }.map { parameter ->
                                parameter.type.toLLVMType(true)
                            }
                        }.orEmpty() +
                        it.fields.map { field ->
                            field.type?.toLLVMType(true) ?: error("field ${field.name.lexeme} has an unknown type")
                        }

                    val structType = context.struct(
                        elementTypes = elementTypes.toTypedArray(),
                        name = it.name.lexeme
                    )

                    env.addType(
                        it.name.lexeme,
                        structType
                    )

                    val (allocType, allocFunction) = module.function(
                        "${it.name.lexeme}_allocate",
                        parameterTypes = emptyList(),
                        returnType = structType.pointer,
                        vararg = false,
                    ) { _ ->
                        basicBlocks.append {
                            val (mallocType, mallocFunction) = env.getFunction("malloc") ?: error("unable to find malloc (???)")

                            val ptr = call(
                                mallocType,
                                mallocFunction.llvmRef,
                                arrayOf(
                                    structType.size
                                )
                            )

                            memset(
                                ptr,
                                context.int8.constInt(0),
                                structType.size,
                            )

                            ret(ptr)
                        }
                    }

                    val primary = it.primaryConstructor
                    val primaryConstructorName = "${it.name.lexeme}_primary"

                    val primaryParametersFields = primary
                        ?.parameters
                        ?.mapIndexedNotNull { i, typedParameter ->
                            typedParameter
                                .takeIf {
                                    primary.parameterTypes[i] != TypedClassDeclaration.FieldType.NONE
                                }
                                ?.let { tp -> tp to i }
                        }
                        .orEmpty()

                    // note: generate a no args only if there are also no secondary constructors
                    // todo: double check that this semantic is matched inside type checker
                    module.function(
                        primaryConstructorName,
                        parameterTypes = primary
                            ?.parameters
                            ?.map { typedParameter ->
                                typedParameter.type.toLLVMType(true)
                            }
                            .orEmpty(),
                        returnType = structType.pointer,
                    ) { functionType ->
                        function(this@function, primaryConstructorName) {
                            env.addFunction(
                                "${it.name.lexeme}/${primary?.overloadSuffix(it.name.lexeme) ?: "//${it.name.lexeme}"}",
                                functionType to this@function
                            )

                            var primaryConstructorBlock = basicBlocks.append {}
                            builder.positionAtEnd(primaryConstructorBlock)

                            val allocated = builder.call(
                                allocType,
                                allocFunction.llvmRef,
                                emptyArray()
                            )

                            primaryParametersFields.forEachIndexed { index, (_, i) ->
                                val field = builder.gepInbounds(
                                    structType,
                                    allocated,
                                    arrayOf(context.int32.constInt(0), context.int32.constInt(index))
                                )

                                builder.store(parameters[i], field)
                            }

                            val offset = primaryParametersFields.size
                            it.fields.forEachIndexed { index, field ->
                                val (fieldValue, nextBlock) = field.initializer?.let { initializer ->
                                    dfs(initializer, builder, primaryConstructorBlock)
                                } ?: error("field ${field.name.lexeme} must have an initializer (currently)")
                                primaryConstructorBlock = nextBlock

                                val field = builder.gepInbounds(
                                    structType,
                                    allocated,
                                    arrayOf(context.int32.constInt(0), context.int32.constInt(index + offset))
                                )

                                builder.store(fieldValue, field)
                            }

                            builder.ret(allocated)
                        }
                    }

                    it.secondaryConstructors.forEach { secondary ->
                        val currentSecondaryConstructorName = "${it.name.lexeme}_secondary"

                        val parameterTypes = secondary.parameters.map { typedParameter -> typedParameter.type.toLLVMType(true) }
                        module.function(
                            currentSecondaryConstructorName,
                            parameterTypes = parameterTypes,
                            returnType = structType.pointer,
                        ) { functionType ->
                            function(this@function, currentSecondaryConstructorName) {
                                env.addFunction(
                                    "${it.name.lexeme}/${secondary.overloadSuffix(it.name.lexeme)}",
                                    functionType to this@function
                                )

                                scope {
                                    secondary.parameters.forEachIndexed { i, p ->
                                        val parameterName = p.name.lexeme
                                        val parameterType = parameterTypes[i]

                                        env.addVariable(parameterName, parameters[i], parameterType, true)
                                    }

                                    var currentBlockCtor = basicBlocks.append {}

                                    val (delegatedConstructorType, delegatedConstructorFunction) = env.getFunction(
                                        "${it.name.lexeme}/${secondary.delegatedSuffix(it.name.lexeme)}"
                                    ) ?: error("TODO: delegated constructor not defined yet (toposort?)")

                                    val arr = Array<Value>(secondary.delegatedArguments.size) { null }

                                    secondary.delegatedArguments.forEachIndexed { i, arg ->
                                        builder.positionAtEnd(currentBlockCtor)
                                        val (argValue, argBlock) = dfs(arg, builder, currentBlockCtor)
                                        arr[i] = argValue
                                        currentBlockCtor = argBlock
                                    }

                                    val ret =
                                        builder.call(
                                            delegatedConstructorType,
                                            delegatedConstructorFunction.llvmRef,
                                            arr
                                        )

                                    env.addVariable("this", ret, structType.pointer, true)

                                    generateStatements(secondary.body, builder, currentBlockCtor)

                                    builder.ret(ret)
                                }
                            }
                        }
                    }

                    val className = it.name.lexeme
                    it.methods.forEach { method ->
                        val receiverType = method.receiver?.toLLVMType(true)
                        val contextTypes = method.contexts.map { c -> c.toLLVMType(true) }
                        val parameterTypes = method.parameters.map { p -> p.type.toLLVMType(true) }

                        val finalParameterTypes = buildList {
                            add(structType.pointer)

                            receiverType?.let { rt ->
                                add(rt)
                            }
                            addAll(contextTypes)
                            addAll(parameterTypes)
                        }

                        val offset = if (receiverType != null) 2 else 1
                        val methodName = "${it.name.lexeme}#${method.name.lexeme}"

                        module.function(
                            name = methodName,
                            parameterTypes = finalParameterTypes,
                            returnType = method.returnType.toLLVMType(true),
                        ) { methodType ->
                            function(this@function, methodName) {
                                env.addTypeMethod(
                                    className,
                                    methodType to this@function
                                )

                                scope {
                                    val b = basicBlocks.append { _ ->
                                        env.addVariable(
                                            "this",
                                            parameters[0],
                                            structType.pointer,
                                            true,
                                        )
                                        // todo: receiver type
                                        method.contexts.forEachIndexed { i, type ->
                                            val contextType = contextTypes[i]

                                            env.addContext(type, parameters[offset + i], contextType)
                                        }
                                        method.parameters.forEachIndexed { i, p ->
                                            val parameterName = p.name.lexeme
                                            val parameterType = parameterTypes[i]

                                            env.addVariable(
                                                parameterName,
                                                parameters[offset + method.contexts.size + i],
                                                parameterType,
                                                true,
                                            )
                                        }
                                    }

                                    builder.positionAtEnd(b)

                                    val previousReturnEmitted = returnEmitted
                                    returnEmitted = false

                                    val b1 = generateStatements(method.body, builder, b)

                                    if (!returnEmitted) {
                                        builder.positionAtEnd(b1)
                                        builder.ret()
                                    }

                                    returnEmitted = previousReturnEmitted
                                }
                            }
                        }
                    }

                    currentBlock
                }
                is TypedFunctionDeclaration -> {
                    val receiverType = it.receiver?.toLLVMType(true)
                    val contextTypes = it.contexts.map { c -> c.toLLVMType(true) }
                    val parameterTypes = it.parameters.map { p -> p.type.toLLVMType(true) }

                    val finalParameterTypes = (receiverType?.let { rt ->
                        listOf(rt)
                    } ?: emptyList()) + contextTypes + parameterTypes

                    val offset = if (receiverType != null) 1 else 0

                    module.function(
                        name = it.name.lexeme,
                        parameterTypes = finalParameterTypes,
                        returnType = it.returnType.toLLVMType(true),
                        vararg = false,
                    ) { type ->
                        function(this@function, it.name.lexeme) {
                            env.addFunction(
                                it.mangledName,
                                type to this@function,
                            )

                            scope {
                                val b = basicBlocks.append { _ ->
                                    receiverType?.let { receiver ->
                                        env.addVariable(
                                            "this",
                                            parameters[0], // note: this will always be the first parameter
                                            receiver,
                                            true
                                        )
                                    }
                                    it.contexts.forEachIndexed { i, type ->
                                        val contextType = contextTypes[i]

                                        env.addContext(type, parameters[i + offset], contextType)
                                    }
                                    it.parameters.forEachIndexed { i, p ->
                                        val parameterName = p.name.lexeme
                                        val parameterType = parameterTypes[i]

                                        env.addVariable(
                                            parameterName,
                                            parameters[i + it.contexts.size + offset],
                                            parameterType,
                                            true
                                        )
                                    }
                                }

                                builder.positionAtEnd(b)

                                val previousReturnEmitted = returnEmitted
                                returnEmitted = false

                                val b1 = generateStatements(it.body, builder, b)

                                if (!returnEmitted) {
                                    builder.positionAtEnd(b1)
                                    builder.ret()
                                }

                                returnEmitted = previousReturnEmitted
                            }
                        }
                    }

                    currentBlock
                }
                is TypedDeleteStatement -> {
                    // do nothing
                    // note: delete statements (currently only allowed for functions) should not be in the generated
                    // bytecode to maintain the zero runtime size cost of deleting functions

                    currentBlock
                }
                is TypedExpressionStatement -> {
                    val (_, newBlock) = dfs(it.expression, builder, currentBlock)

                    newBlock
                }
                is TypedIfStatement -> {
                    val currentFunction = function ?: error("if statements can only be in function bodies (should be unreachable)")

                    val (cond, condBlock) = dfs(it.condition, builder, currentBlock)

                    builder.positionAtEnd(condBlock)

                    var thenBranchBlock = currentFunction.basicBlocks.append("if_then") {}
                    var elseBranchBlock = currentFunction.basicBlocks.append("if_else") {}

                    builder.cond(cond, thenBranchBlock, elseBranchBlock)

                    val previousReturnEmitted = returnEmitted

                    returnEmitted = false
                    thenBranchBlock = generateStatements(it.trueBranch, builder, thenBranchBlock)

                    val thenBranchReturned = returnEmitted

                    returnEmitted = false
                    elseBranchBlock = generateStatements(it.falseBranch, builder, elseBranchBlock)

                    val elseBranchReturned = returnEmitted

                    returnEmitted = previousReturnEmitted

                    if (thenBranchReturned && elseBranchReturned) {
                        returnEmitted = true

                        elseBranchBlock
                    } else if (thenBranchReturned && it.falseBranch.isEmpty()) {
                        elseBranchBlock
                    } else {
                        val endBlock = currentFunction.basicBlocks.append("if_end") {}

                        if (!thenBranchReturned) {
                            builder.positionAtEnd(thenBranchBlock)
                            builder.br(endBlock)
                        }

                        if (!elseBranchReturned) {
                            builder.positionAtEnd(elseBranchBlock)
                            builder.br(endBlock)
                        }

                        endBlock
                    }
                }
                is TypedReturnExpressionStatement -> {
                    returnEmitted = true

                    val returnType = it.returnExpression.type

                    val (value, nextBlock) = dfs(it.returnExpression, builder, currentBlock)

                    builder.positionAtEnd(nextBlock)

                    if (returnType is VariableType && returnType.mangledName == "Unit") {
                        builder.ret()
                    } else {
                        builder.ret(value)
                    }

                    nextBlock
                }
                is TypedReturnStatement -> {
                    returnEmitted = true

                    it.value?.let { returnValue ->
                        val (retValue, newBlock) = dfs(returnValue, builder, currentBlock)

                        builder.positionAtEnd(newBlock)

                        builder.ret(retValue)

                        newBlock
                    } ?: run {
                        builder.ret()

                        currentBlock
                    }
                }
                is TypedVariableStatement -> {
                    val variableName = it.name.lexeme
                    val variableType = it.type?.toLLVMType(true) ?: error("type of ${it.name.lexeme} is null (should be unreachable)")
                    val location = builder.alloca(variableType, variableName)

                    env.addVariable(variableName, location, variableType, false)

                    it.initializer?.let { initializer ->
                        val (value, newBlock) = dfs(initializer, builder, block)

                        builder.positionAtEnd(newBlock)

                        builder.store(value, location)

                        newBlock
                    } ?: currentBlock
                }
                is TypedWhileStatement -> {
                    val currentFunction = function ?: error("if statements can only be in function bodies (should be unreachable)")

                    val condBlock = currentFunction.basicBlocks.append("while_cond") {}
                    var bodyBlock = currentFunction.basicBlocks.append("while_body") {}
                    val endBlock = currentFunction.basicBlocks.append("while_end") {}

                    builder.br(condBlock)

                    val (cond, condBlock2) = dfs(it.condition, builder, condBlock)

                    builder.positionAtEnd(condBlock2)

                    builder.cond(cond, bodyBlock, endBlock)

                    bodyBlock = generateStatements(it.body, builder, bodyBlock)

                    builder.positionAtEnd(bodyBlock)
                    builder.br(condBlock)

                    endBlock
                }
            }
        }

        return currentBlock
    }

    private fun dfs(root: TypedExpression, builder: Builder, block: BasicBlock): Pair<Value, BasicBlock> {
        builder.positionAtEnd(block)
        return when (root) {
            is TypedAssign -> {
                val name = root.name.lexeme
                val (value, newBlock) = dfs(root.expression, builder, block)
                val (dest, _, param) = env.getVariable(name) ?: error("No variable found for $name")

                if (param) {
                    error("unable to assign to $name as it is a function parameter")
                }

                builder.positionAtEnd(newBlock)

                builder.store(value, dest) to newBlock
            }
            is TypedBinary -> {
                val (lhs, b1) = dfs(root.left, builder, block)
                val (rhs, b2) = dfs(root.right, builder, b1)

                val type = root.left.type // NOTE: currently assume both types match

                val value = when (root.operator.type) {
                    TokenType.PLUS -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double", "Int", "Long" -> {
                                        builder.add(lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.MINUS -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double", "Int", "Long" -> {
                                        builder.sub(lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.STAR -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fmul(lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.mul(lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.SLASH -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fdiv(lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.sdiv(lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.MOD -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.frem(lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.srem(lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.EQUALS -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealOEQ, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntEQ, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.NOT_EQ -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealONE, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntNE, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.GE -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealOGE, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntSGE, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.LE -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealOLE, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntSLE, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.GT -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealOGT, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntSGT, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    TokenType.LT -> {
                        when (type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fcmp(LLVMRealPredicate.LLVMRealOLT, lhs, rhs)
                                    }
                                    "Int", "Long" -> {
                                        builder.icmp(LLVMIntPredicate.LLVMIntSLT, lhs, rhs)
                                    }
                                    else -> {
                                        error("invalid binary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid binary operator type") // should be unreachable
                            }
                        }
                    }
                    else -> error("invalid binary operator")
                }

                value to b2
            }
            is TypedCall -> {
                if (root.callee is TypedGet && root.callee.slot != -1) { // note: calling a method
                    val typedGet = root.callee
                    val instance = root.callee.instance
                    val instanceType = instance.type

                    val (funcType, funcVal) = env.getTypeMethod(
                        instanceType.mangledName,
                        // NOTE: calling a method, - 1 accounting for the "constructor method"
                        typedGet.slot - 1
                    ) ?: error("method ${typedGet.name.lexeme} not found on $instanceType")

                    var currentBlock = block
                    val argumentTypes = mutableListOf<Type>()
                    val arguments = Array(root.arguments.size + 1) {
                        val (value, b) = if (it == 0) {
                            argumentTypes.add(instanceType.toLLVMType(true))
                            dfs(instance, builder, currentBlock)
                        } else {
                            val argument = root.arguments[it + 1]
                            argumentTypes.add(argument.type.toLLVMType(true))
                            dfs(argument, builder, currentBlock)
                        }

                        currentBlock = b

                        value
                    }

                    builder.call(
                        funcType,
                        funcVal.llvmRef,
                        arguments
                    ) to currentBlock
                } else {
                    val (func, b1) = dfs(root.callee, builder, block)
                    var currentBlock = b1
                    val argumentTypes = mutableListOf<Type>()
                    val arguments = Array(root.arguments.size) {
                        val argument = root.arguments[it]
                        argumentTypes.add(argument.type.toLLVMType(true))
                        val (value, b) = dfs(argument, builder, currentBlock)

                        currentBlock = b

                        value
                    }
                    val value = builder.call(
                        functionType = Type.Function(
                            context,
                            argumentTypes,
                            root.type.toLLVMType(true)
                        ),
                        function = func,
                        args = arguments,
                    )

                    value to currentBlock
                }
            }
            is TypedContextVariable -> {
                val (cValue, _) = env.getContext(root.type) ?: error("context ${root.type} not in scope (should be unreachable)")

                // NOTE: currently contexts can only be introduced as parameters to a function, so no loads are needed
                //       currently when accessing them
                cValue to block
            }
            is TypedVariable -> {
                val name = root.mangledName
                val value = when (val type = root.type) {
                    is ClassType -> {
                        // NOTE: name (mangledName) should be the name of the constructor we want
                        val function = env.getFunction(name)
                        function?.second?.llvmRef ?: error("no constructor for type ${type.name} ($name) found (should be unreachable)")
                    }
                    is FunctionType -> {
                        val function = env.getFunction(name)
                        function?.second?.llvmRef ?: error("no function $name found (should be unreachable)")
                    }
                    is LambdaType -> {
                        val variable = env.getVariable(name)
                        variable?.let { (value, type, parameter) ->
                            if (parameter) {
                                value
                            } else {
                                builder.load(type, value, name)
                            }
                        } ?: error("No lambda variable found for $name")
                    }
                    is VariableType -> {
                        val variable = env.getVariable(name)
                        variable?.let { (value, type, parameter) ->
                            if (parameter) {
                                value
                            } else {
                                builder.load(type, value, name)
                            }
                        } ?: error("No variable found for $name")
                    }
                }

                value to block
            }
            is TypedGet -> {
                val instance = root.instance

                when (instance.type) {
                    is ClassType -> {
                        val (inst, newBlock) = dfs(instance, builder, block)

                        builder.positionAtEnd(newBlock)

                        builder.load(
                            root.type.toLLVMType(false),
                            builder.gep(
                                instance.type.toLLVMType(false),
                                pointer = inst,
                                indices = arrayOf(context.int32.constInt(0), context.int32.constInt(root.slot)),
                            )
                        ) to newBlock
                    }
                    is FunctionType -> TODO("function type get not implemented")
                    is LambdaType -> {
                        if (root.name.lexeme == "invoke") {
                            dfs(instance, builder, block)
                        } else {
                            TODO("lambda type get (for non-invoke) not implemented")
                        }
                    }
                    is VariableType -> {
                        if (root.type is VariableType) {
                            val (inst, newBlock) = dfs(instance, builder, block)

                            builder.positionAtEnd(newBlock)

                            // todo: assuming that this was a property get as the instance type is currently not being
                            //       merged with existing class types. hopefully, root.slot will always be the correct
                            //       gep index value
                            builder.load(
                                root.type.toLLVMType(false),
                                builder.gep(
                                    instance.type.toLLVMType(false),
                                    pointer = inst,
                                    indices = arrayOf(context.int32.constInt(0), context.int32.constInt(root.slot)),
                                )
                            ) to newBlock
                        } else {
                            TODO("variable type get is not implemented")
                        }
                    }
                }
            }
            is TypedGrouping -> {
                dfs(root.expression, builder, block)
            }
            is TypedIfExpression -> TODO()
            is TypedInlineCall -> TODO()
            is TypedLambda -> {
                require(root.captures.isEmpty()) {
                    "closures currently not supported on LLVM backend"
                }

                val contextTypes = root.contexts.map { it.toLLVMType(true) }
                val parameterTypes = root.parameters.map { it.type.toLLVMType(true) }

                val lambdaType = root.type.toLLVMType(false)

                val lambdaName = "${functionName}_lambda_${root.contexts.size + root.parameters.size}"

                val (lambdaLLVMType, lambdaLLVMValue) = module.function(
                    name = "lambda",
                    functionType = lambdaType,
                ) { type ->
                    function(this@function, lambdaName) {
                        scope {
                            val b = basicBlocks.append { _ ->
                                root.contexts.forEachIndexed { i, type ->
                                    val contextType = contextTypes[i]

                                    env.addContext(type, parameters[i], contextType)
                                }
                                root.parameters.forEachIndexed { i, p ->
                                    val parameterName = p.name.lexeme
                                    val parameterType = parameterTypes[i]

                                    env.addVariable(
                                        parameterName,
                                        parameters[i + root.contexts.size],
                                        parameterType,
                                        true
                                    )
                                }
                            }

                            builder.positionAtEnd(b)

                            val previousReturnEmitted = returnEmitted
                            returnEmitted = false

                            val b1 = generateStatements(root.body, builder, b)

                            if (!returnEmitted) {
                                builder.positionAtEnd(b1)
                                builder.ret()
                            }

                            returnEmitted = previousReturnEmitted
                        }
                    }
                }

                builder.positionAtEnd(block)

                lambdaLLVMValue.llvmRef to block
            }
            is TypedBooleanLiteral -> {
                context.int1.constInt(if (root.value) 1 else 0) to block
            }
            is TypedDoubleLiteral -> {
                context.double.constDouble(root.value) to block
            }
            is TypedIntLiteral -> {
                context.int32.constInt(root.value) to block
            }
            is TypedLongLiteral -> {
                context.int64.constInt(root.value) to block
            }
            TypedNullLiteral -> {
                // TODO: what is different between const null and const null pointer?
                context.void.pointer.constNull() to block
            }
            is TypedStringLiteral -> {
                builder.globalStringPointer(root.value) to block
            }
            is TypedLogical -> {
                val currentFunction = function ?: error("if statements can only be in function bodies (should be unreachable)")

                val (lhs, lhsBlock) = dfs(root.left, builder, block)

                builder.positionAtEnd(lhsBlock)

                when (root.operator.type) {
                    TokenType.AND -> {
                        val rhsBlock = currentFunction.basicBlocks.append("and_rhs") {}
                        val andBlock = currentFunction.basicBlocks.append("and_end") {}

                        builder.cond(lhs, rhsBlock, andBlock)

                        val (rhs, rhsBlock2) = dfs(root.right, builder, rhsBlock)

                        builder.positionAtEnd(rhsBlock2)
                        builder.br(andBlock)

                        builder.positionAtEnd(andBlock)
                        val phiAnd = builder.phi(
                            context.int1,
                            arrayOf(block, rhsBlock),
                            arrayOf(context.int1.constInt(0), rhs)
                        )

                        phiAnd to andBlock
                    }
                    TokenType.OR -> {
                        val rhsBlock = currentFunction.basicBlocks.append("or_rhs") {}
                        val orBlock = currentFunction.basicBlocks.append("or_end") {}

                        builder.cond(lhs, orBlock, rhsBlock)

                        val (rhs, rhsBlock2) = dfs(root.right, builder, rhsBlock)

                        builder.positionAtEnd(rhsBlock2)
                        builder.br(orBlock)

                        builder.positionAtEnd(orBlock)
                        val phiOr = builder.phi(
                            context.int1,
                            arrayOf(block, rhsBlock),
                            arrayOf(context.int1.constInt(1), rhs),
                        )

                        phiOr to orBlock
                    }
                    else -> error("invalid logical operator found (should be unreachable)")
                }
            }
            is TypedSet -> {
                val (inst, instBlock) = dfs(root.instance, builder, block)
                val (expr, nextBlock) = dfs(root.expression, builder, instBlock)

                builder.positionAtEnd(nextBlock)

                builder.store(
                    expr,
                    builder.gep(
                        root.instance.type.toLLVMType(false),
                        pointer = inst,
                        indices = arrayOf(context.int32.constInt(0), context.int32.constInt(root.slot)),
                    )
                ) to nextBlock
            }
            is TypedThis -> {
                val (thisValue, _, thisParameter) = env.getVariable("this") ?: error("accessing this inside a scope without unqualified this (should be unreachable)")

                require(thisParameter) {
                    "this is not a parameter somehow (should be unreachable)"
                }

                thisValue to block
            }
            is TypedUnary -> {
                val (expr, exprBlock) = dfs(root.expression, builder, block)

                builder.positionAtEnd(exprBlock)

                when (root.operator.type) {
                    TokenType.PLUS -> {
                        // note: since we have passed type checking, the types here should be fine
                        expr
                    }
                    TokenType.MINUS -> {
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        builder.fneg(expr)
                                    }
                                    "Int", "Long" -> {
                                        builder.neg(expr)
                                    }
                                    else -> error("invalid unary operator type (should be unreachable)")
                                }
                            }
                            else -> error("invalid unary operator type (should be unreachable)")
                        }
                    }
                    TokenType.NOT -> {
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Boolean" -> {
                                        builder.not(expr)
                                    }
                                    else -> error("invalid unary operator type (should be unreachable)")
                                }
                            }
                            else -> error("invalid unary operator type (should be unreachable)")
                        }
                    }
                    else -> error("invalid unary operator (should be unreachable)")
                } to exprBlock
            }
        }
    }

    private fun analysis.ast.Type.toLLVMType(convertToPointer: Boolean): Type {
        return when (val type = this) {
            is ClassType -> {
                env.getType(type.name)?.let {
                    if (convertToPointer) {
                        it.pointer
                    } else {
                        it
                    }
                } ?: error("unknown type ${type.name}")
            }
            is FunctionType -> TODO()
            is LambdaType -> {
                Type.Function(
                    context,
                    parameterTypes = type.contextTypes.map { it.toLLVMType(true) } + type.parameterTypes.map { it.toLLVMType(true) },
                    returnType = type.returnType.toLLVMType(true),
                    vararg = false,
                ).let {
                    if (convertToPointer) {
                        it.pointer
                    } else {
                        it
                    }
                }
            }
            is VariableType -> {
                when (type.name) {
                    "Boolean" -> context.int1
                    "Double" -> context.double
                    "Int" -> context.int32
                    "Long" -> context.int64
                    "Unit" -> context.void
                    "String" -> context.int8.pointer
                    else -> {
                        env.getType(type.name)?.let {
                            if (convertToPointer) {
                                it.pointer
                            } else {
                                it
                            }
                        } ?: error("unknown type ${type.name}")
                    }
                }
            }
        }
    }

    private inline fun scope(block: () -> Unit) {
        val previousEnv = env
        env = LLVMEnvironment(env)

        block()

        env = previousEnv
    }

    private inline fun function(newFunction: Function, newFunctionName: String, block: () -> Unit) {
        val previousFunction = function
        val previousFunctionName = functionName

        function = newFunction
        functionName = newFunctionName

        block()

        function = previousFunction
        functionName = previousFunctionName
    }

    private fun generateMangledName(name: String, parameterTypes: List<String>, returnType: String): String {
        return "$name//${parameterTypes.joinToString("|")}/$returnType"
    }

    public fun type(block: Context.() -> Type): Type {
        return context.block()
    }

    public fun getNativeFunction(name: String): Pair<Type, Function>? {
        return env.getFunction(name)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class LLVMEnvironment(val enclosing: LLVMEnvironment?) {
    private val variables: MutableMap<String, Triple<Value, Type, Boolean>> = mutableMapOf()
    private val contexts: MutableMap<analysis.ast.Type, Pair<Value, Type>> = mutableMapOf()
    private val functions: MutableMap<String, Pair<Type, Function>> = mutableMapOf()
    private val types: MutableMap<String, Type> = mutableMapOf()
    private val methods: MutableMap<String, MutableList<Pair<Type, Function>>> = mutableMapOf()

    fun addVariable(name: String, value: Value, type: Type, parameter: Boolean) {
        if (variables.containsKey(name)) {
            error("duplicate variable $name exists in scope")
        }
        variables[name] = Triple(value, type, parameter)
    }

    fun getVariable(name: String): Triple<Value, Type, Boolean>? {
        return this.variables.getOrElse(name) {
            this.enclosing?.getVariable(name)
        }
    }

    fun addContext(astType: analysis.ast.Type, value: Value, type: Type) {
        if (contexts.containsKey(astType)) {
            error("duplicate context of type $astType exists in scope")
        }
        contexts[astType] = value to type
    }

    fun getContext(astType: analysis.ast.Type): Pair<Value, Type>? {
        return this.contexts.getOrElse(astType) {
            this.enclosing?.getContext(astType)
        }
    }

    fun addFunction(name: String, function: Pair<Type, Function>) {
        if (functions.containsKey(name)) {
            error("duplicate function $name exists in scope")
        }
        functions[name] = function
    }

    fun getFunction(name: String): Pair<Type, Function>? {
        return this.functions.getOrElse(name) {
            this.enclosing?.getFunction(name)
        }
    }

    fun addType(name: String, type: Type) {
        if (types.containsKey(name)) {
            error("duplicate type $name exists in scope")
        }
        types[name] = type
    }

    fun getType(name: String): Type? {
        return this.types.getOrElse(name) {
            this.enclosing?.getType(name)
        }
    }

    fun addTypeMethod(className: String, method: Pair<Type, Function>) {
        this.methods.getOrPut(className) {
            mutableListOf()
        }.add(method)
    }

    fun getTypeMethod(className: String, slot: Int): Pair<Type, Function>? {
        return this.methods[className]?.getOrNull(slot) ?: this.enclosing?.getTypeMethod(className, slot)
    }
}