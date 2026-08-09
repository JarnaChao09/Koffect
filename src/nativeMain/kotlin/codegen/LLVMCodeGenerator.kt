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
                is TypedClassDeclaration -> TODO()
                is TypedFunctionDeclaration -> {
                    val previousFunction = function

                    val parameterTypes = it.parameters.map { p -> p.type.toLLVMType() }
                    module.function(
                        name = it.name.lexeme,
                        parameterTypes = parameterTypes,
                        returnType = it.returnType.toLLVMType(),
                        vararg = false,
                    ) { type ->
                        function = this@function
                        env.addFunction(
                            it.mangledName,
                            type to this@function,
                        )

                        scope {
                            val b = basicBlocks.append { _ ->
                                it.parameters.forEachIndexed { i, p ->
                                    val parameterName = p.name.lexeme
                                    val parameterType = parameterTypes[i]

                                    env.addVariable(parameterName, parameters[i], parameterType, true)
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
                    function = previousFunction

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
                    TODO()
                    // builder.positionAtEnd(block)
                    //
                    // val (retValue, newBlock) = dfs(it.returnExpression, builder, block)
                    //
                    // builder.positionAtEnd(newBlock)
                    //
                    // builder.ret(retValue)
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
                    val variableType = it.type?.toLLVMType() ?: error("type of ${it.name.lexeme} is null (should be unreachable)")
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
                                    "Double", "Int" -> {
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
                                    "Double", "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                                    "Int" -> {
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
                val (func, b1) = dfs(root.callee, builder, block)
                var currentBlock = b1
                val argumentTypes = mutableListOf<Type>()
                val arguments = Array(root.arguments.size) {
                    val argument = root.arguments[it]
                    argumentTypes.add(argument.type.toLLVMType())
                    val (value, b) = dfs(argument, builder, currentBlock)

                    currentBlock = b

                    value
                }
                val value = builder.call(
                    functionType = Type.Function(
                        context,
                        argumentTypes,
                        root.type.toLLVMType()
                    ),
                    function = func,
                    args = arguments,
                )

                value to b1
            }
            is TypedContextVariable -> TODO()
            is TypedVariable -> {
                val name = root.mangledName
                val value = when (val type = root.type) {
                    is ClassType -> TODO("lookup of classes not supported")
                    is FunctionType -> {
                        val function = env.getFunction(name)
                        function?.second?.llvmRef ?: error("no function $name found (should be unreachable)")
                    }
                    is LambdaType -> TODO("lookup of lambdas not supported")
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
            is TypedGet -> TODO()
            is TypedGrouping -> {
                dfs(root.expression, builder, block)
            }
            is TypedIfExpression -> TODO()
            is TypedInlineCall -> TODO()
            is TypedLambda -> TODO()
            is TypedBooleanLiteral -> TODO()
            is TypedDoubleLiteral -> TODO()
            is TypedIntLiteral -> {
                context.int32.constInt(root.value) to block
            }
            TypedNullLiteral -> TODO()
            is TypedStringLiteral -> {
                builder.globalStringPointer(root.value) to block
            }
            is TypedLogical -> TODO()
            is TypedSet -> TODO()
            is TypedThis -> TODO()
            is TypedUnary -> TODO()
        }
    }

    private fun analysis.ast.Type.toLLVMType(): Type {
        return when (val type = this) {
            is ClassType -> TODO()
            is FunctionType -> TODO()
            is LambdaType -> TODO()
            is VariableType -> {
                when (type.name) {
                    "Boolean" -> context.int1
                    "Int" -> context.int32
                    "Unit" -> context.void
                    "String" -> context.int8.pointer
                    else -> TODO()
                }
            }
        }
    }

    private fun scope(block: () -> Unit) {
        val previousEnv = env
        env = LLVMEnvironment(env)

        block()

        env = previousEnv
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
    private val functions: MutableMap<String, Pair<Type, Function>> = mutableMapOf()

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
}