package codegen

import analysis.ast.*
import lexer.TokenType
import runtime.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class CodeGenerator {
    private lateinit var currentChunk: Chunk
    private var line: Int = 1
    private var returnEmitted: Boolean = false
    private val stack: LocalsStack = LocalsStack()
    private var inlinedParameters: Map<String, TypedExpression> = emptyMap()
    private var inlinedJumps: MutableList<Int> = mutableListOf()

    public fun generate(ast: List<TypedStatement>): Chunk {
        this.currentChunk = Chunk()

        this.generateStatements(ast, false)

        this.currentChunk.write(Opcode.Null.toInt(), this.line++)
        this.currentChunk.write(Opcode.Return.toInt(), this.line++)

        return this.currentChunk
    }

    private fun generateStatements(ast: List<TypedStatement>, inline: Boolean) {
        ast.forEach {
            when(it) {
                is TypedClassDeclaration -> {
                    TODO()
                }
                is TypedExpressionStatement -> {
                    dfs(it.expression, inline)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                }
                is TypedReturnExpressionStatement -> {
                    dfs(it.returnExpression, inline)
                }
                is TypedIfStatement -> {
                    this.generateIf(it.condition, it.trueBranch, it.falseBranch, inline)
                }
                is TypedFunctionDeclaration -> {
                    if (inline) {
                        error("Nested function declarations cannot be codegen-ed for inline functions")
                    }
                    if (!it.deleted && !it.inline) {
                        val binding = this.currentChunk.addConstant(it.mangledName.toValue())

                        val oldChunk = this.currentChunk
                        val previousReturnEmitted = this.returnEmitted

                        this.currentChunk = Chunk()
                        this.returnEmitted = false

                        val function: ObjectFunction

                        this.stack.withNewScope {
                            it.contexts.forEach { ctx ->
                                this.stack.addContextVariable(ctx)
                            }
                            it.parameters.forEach { parameter ->
                                this.stack.addVariable(parameter.name.lexeme)
                            }

                            this.generateStatements(it.body, inline)

                            if (!returnEmitted) {
                                val constant = this.currentChunk.addConstant(UnitValue)

                                this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                                this.currentChunk.write(constant, this.line++)

                                this.currentChunk.write(Opcode.Return.toInt(), this.line++)
                            }

                            function = ObjectFunction(Function(it.mangledName, it.arity, this.currentChunk))

                            this.currentChunk = oldChunk
                            this.returnEmitted = previousReturnEmitted
                        }

                        val constant = this.currentChunk.addConstant(function)
                        this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                        this.currentChunk.write(constant, this.line++)

                        this.currentChunk.write(Opcode.DefineGlobal.toInt(), this.line)
                        this.currentChunk.write(binding, this.line++)
                    }
                }
                is TypedVariableStatement -> {
                    it.initializer?.let { expr ->
                        dfs(expr, inline)
                    } ?: this.currentChunk.write(Opcode.Null.toInt(), this.line++)

                    if (this.stack.inGlobalScope()) {
                        val binding = this.currentChunk.addConstant(it.name.lexeme.toValue())

                        this.currentChunk.write(Opcode.DefineGlobal.toInt(), this.line)
                        this.currentChunk.write(binding, this.line++)
                    } else {
                        // if inline is true, the variable will need to be mangled
                        this.currentChunk.write(Opcode.SetLocal.toInt(), this.line)
                        this.currentChunk.write(
                            this.stack.addVariable(it.name.lexeme),
                            this.line
                        )
                        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                    }
                }
                is TypedWhileStatement -> {
                    val loopStart = this.currentChunk.code.size

                    this.dfs(it.condition, inline)

                    val exitJump = this.currentChunk.emitJump(Opcode.JumpIfFalse)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                    this.stack.withNestedScope {
                        this.generateStatements(it.body, inline)
                    }

                    this.currentChunk.patchLoop(loopStart)

                    this.currentChunk.patchJump(exitJump)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                }
                is TypedReturnStatement -> {
                    this.returnEmitted = true

                    it.value?.let { returnValue ->
                        this.dfs(returnValue, inline)
                    } ?: run {
                        val constant = this.currentChunk.addConstant(UnitValue)
                        this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                        this.currentChunk.write(constant, this.line)
                    }

                    if (inline) {
                        this.inlinedJumps.add(
                            this.currentChunk.emitJump(Opcode.Jump)
                        )
                    } else {
                        this.currentChunk.write(Opcode.Return.toInt(), this.line++)
                    }
                }
                is TypedDeleteStatement -> {
                    // do nothing
                    // note: delete statements (currently only allowed for functions) should not be in the generated
                    // bytecode to maintain the zero runtime size cost of deleting functions
                    // todo: this will be different once the IR is rewritten to allow for dynamic-ish linking (pending spec)
                }
            }
        }
    }

    private fun dfs(root: TypedExpression, inline: Boolean) {
        when (root) {
            is TypedAssign -> {
                dfs(root.expression, inline)

                if (this.stack.inGlobalScope() || !this.stack.isLocal(root.name.lexeme)) {
                    val binding = this.currentChunk.addConstant(root.name.lexeme.toValue())

                    this.currentChunk.write(Opcode.SetGlobal.toInt(), this.line)
                    this.currentChunk.write(binding, this.line++)
                } else {
                    this.currentChunk.write(Opcode.SetLocal.toInt(), this.line)
                    this.currentChunk.write(
                        this.stack.getVariable(root.name.lexeme),
                        this.line++
                    )
                }
            }
            is TypedBinary -> {
                dfs(root.left, inline)
                dfs(root.right, inline)

                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> {
                        when (val type = root.type ) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleAdd
                                    }
                                    "Int" -> {
                                        Opcode.IntAdd
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
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleSubtract
                                    }
                                    "Int" -> {
                                        Opcode.IntSubtract
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
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleMultiply
                                    }
                                    "Int" -> {
                                        Opcode.IntMultiply
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
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleDivide
                                    }
                                    "Int" -> {
                                        Opcode.IntDivide
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
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleMod
                                    }
                                    "Int" -> {
                                        Opcode.IntMod
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleEquals
                                    }
                                    "Int" -> {
                                        Opcode.IntEquals
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleNotEq
                                    }
                                    "Int" -> {
                                        Opcode.IntNotEq
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleGreaterEq
                                    }
                                    "Int" -> {
                                        Opcode.IntGreaterEq
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleLessEq
                                    }
                                    "Int" -> {
                                        Opcode.IntLessEq
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleGreaterThan
                                    }
                                    "Int" -> {
                                        Opcode.IntGreaterThan
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
                        when (val type = root.left.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleLessThan
                                    }
                                    "Int" -> {
                                        Opcode.IntLessThan
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
                }.toInt(), this.line++)
            }
            is TypedCall -> {
                dfs(root.callee, inline)

                val argCount = root.arguments.size
                root.arguments.forEach { this.dfs(it, inline) }

                this.currentChunk.write(Opcode.Call.toInt(), this.line)
                this.currentChunk.write(argCount, this.line++)
            }
            is TypedInlineCall -> {
                this.stack.withNestedScope {
                    val inlinedParameterMap = buildMap {
                        root.inlinedParameterNames.forEachIndexed { index, parameter ->
                            val name = parameter.name.lexeme
                            this@CodeGenerator.stack.addVariable(name) // todo: perhaps do some mangling?
                            put(name, root.arguments[index])
                        }
                    }

                    val previousReturnEmitted = this.returnEmitted
                    val previousInlinedParameters = this.inlinedParameters
                    val previousInlineJumps = this.inlinedJumps

                    this.returnEmitted = false
                    this.inlinedJumps = mutableListOf()
                    this.inlinedParameters = inlinedParameterMap
                    this.generateStatements(root.inlinedBody, inline = true)

                    if (!returnEmitted) {
                        val constant = this.currentChunk.addConstant(UnitValue)

                        this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                        this.currentChunk.write(constant, this.line++)
                    } else {
                        this.inlinedJumps.forEach {
                            this.currentChunk.patchJump(it)
                        }
                    }

                    this.returnEmitted = previousReturnEmitted
                    this.inlinedParameters = previousInlinedParameters
                    this.inlinedJumps = previousInlineJumps
                }
            }
            is TypedGet -> {
                val instance = root.instance
                val name = root.name.lexeme

                when (instance.type) {
                    is FunctionType -> TODO()
                    is LambdaType -> {
                        if (name == "invoke") {
                            this.dfs(instance, inline)
                        } else {
                            TODO()
                        }
                    }
                    is VariableType -> TODO()
                }
            }
            is TypedGrouping -> {
                dfs(root.expression, inline)
            }
            is TypedIfExpression -> {
                this.generateIf(root.condition, root.trueBranch, root.falseBranch, inline)
            }
            is TypedDoubleLiteral -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.DoubleConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is TypedIntLiteral -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.IntConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is TypedBooleanLiteral -> {
                this.currentChunk.write(when (root.value) {
                    true -> Opcode.True
                    false -> Opcode.False
                }.toInt(), this.line++)
            }
            TypedNullLiteral -> {
                this.currentChunk.write(Opcode.Null.toInt(), this.line++)
            }
            is TypedStringLiteral -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is TypedLambda -> {
                val oldChunk = this.currentChunk
                val previousReturnEmitted = this.returnEmitted

                this.currentChunk = Chunk()
                this.returnEmitted = false

                val function: ObjectFunction

                this.stack.withNewScope {
                    root.contexts.forEach { ctx ->
                        this.stack.addContextVariable(ctx)
                    }
                    root.parameters.forEach { parameter ->
                        this.stack.addVariable(parameter.name.lexeme)
                    }

                    this.generateStatements(root.body, inline)

                    if (!returnEmitted) {
                        val constant = this.currentChunk.addConstant(UnitValue)

                        this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                        this.currentChunk.write(constant, this.line++)

                        this.currentChunk.write(Opcode.Return.toInt(), this.line++)
                    }

                    // todo: update ObjectFunction to be able to store lambdas

                    val arity = root.contexts.size + root.parameters.size

                    function = ObjectFunction(Function("Function${arity}", arity, this.currentChunk))

                    this.currentChunk = oldChunk
                    this.returnEmitted = previousReturnEmitted
                }

                val constant = this.currentChunk.addConstant(function)
                this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is TypedLogical -> {
                dfs(root.left, inline)

                val jumpType = when (root.operator.type) {
                    TokenType.AND -> Opcode.JumpIfFalse
                    TokenType.OR -> Opcode.JumpIfTrue
                    else -> error("Invalid Logical Operator") // should be unreachable
                }

                val jump = this.currentChunk.emitJump(jumpType)

                this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                dfs(root.right, inline)

                this.currentChunk.patchJump(jump)
            }
            is TypedThis -> {
                TODO()
            }
            is TypedUnary -> {
                dfs(root.expression, inline)

                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> {
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleIdentity
                                    }
                                    "Int" -> {
                                        Opcode.IntIdentity
                                    }
                                    else -> {
                                        error("invalid unary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid unary operator type")
                            }
                        }
                    }
                    TokenType.MINUS -> {
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Double" -> {
                                        Opcode.DoubleNegate
                                    }
                                    "Int" -> {
                                        Opcode.IntNegate
                                    }
                                    else -> {
                                        error("invalid unary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid unary operator type")
                            }
                        }
                    }
                    TokenType.NOT -> {
                        when (val type = root.type) {
                            is VariableType -> {
                                when (type.name) {
                                    "Boolean" -> {
                                        Opcode.Not
                                    }
                                    else -> {
                                        error("Invalid unary operator type") // should be unreachable
                                    }
                                }
                            }
                            else -> {
                                error("Invalid unary operator type")
                            }
                        }
                    }
                    else -> error("invalid unary operator")
                }.toInt(), this.line++)
            }
            is TypedVariable -> {
                if (this.stack.inGlobalScope() || !this.stack.isLocal(root.mangledName)) {
                    val binding = this.currentChunk.addConstant(root.mangledName.toValue())

                    this.currentChunk.write(Opcode.GetGlobal.toInt(), this.line)
                    this.currentChunk.write(binding, this.line++)
                } else {
                    if (inline) { // perform lazy initialization of parameter values instead
                        val name = root.mangledName

                        if (name in this.inlinedParameters) {
                            val expr = this.inlinedParameters[name] ?: error("$name not found in inlined parameters (should be unreachable)")

                            dfs(expr, inline)
                        } else {
                            this.currentChunk.write(Opcode.GetLocal.toInt(), this.line)
                            this.currentChunk.write(
                                this.stack.getVariable(root.mangledName),
                                this.line++
                            )
                        }
                    } else {
                        this.currentChunk.write(Opcode.GetLocal.toInt(), this.line)
                        this.currentChunk.write(
                            this.stack.getVariable(root.mangledName),
                            this.line++
                        )
                    }
                }
            }
            is TypedContextVariable -> {
                // context variables should only be in local scope
                require(!this.stack.inGlobalScope()) {
                    "context variable should not be accessible from global scope (should be unreachable)"
                }

                this.currentChunk.write(Opcode.GetLocal.toInt(), this.line)
                this.currentChunk.write(
                    this.stack.getContextVariable(root.type),
                    this.line++
                )
            }
        }
    }

    private fun generateIf(condition: TypedExpression, trueBranch: List<TypedStatement>, falseBranch: List<TypedStatement>, inline: Boolean) {
        this.dfs(condition, inline)

        val elseBranch = this.currentChunk.emitJump(Opcode.JumpIfFalse)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.stack.withNestedScope {
            this.generateStatements(trueBranch, inline)
        }

        val skipElseBranch = this.currentChunk.emitJump(Opcode.Jump)
        this.currentChunk.patchJump(elseBranch)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.stack.withNestedScope {
            this.generateStatements(falseBranch, inline)
        }

        this.currentChunk.patchJump(skipElseBranch)
    }

    private fun Chunk.emitJump(instruction: Opcode): Int {
        this.write(instruction.toInt(), this@CodeGenerator.line)
        this.write(Int.MAX_VALUE, this@CodeGenerator.line++)
        return this.code.size - 1
    }

    private fun Chunk.patchJump(offset: Int) {
        val jump = this.code.size - offset - 1

        this.code[offset] = jump
    }

    private fun Chunk.patchLoop(loopStart: Int) {
        this.write(Opcode.Jump.toInt(), this@CodeGenerator.line)

        val offset = this.code.size - loopStart + 1

        this.write(-offset, this@CodeGenerator.line++)
    }
}

public class LocalsStack {
    public val stack: ArrayDeque<MutableMap<String, Int>> = ArrayDeque()
    public val locals: ArrayDeque<MutableMap<String, Int>> = ArrayDeque()
    public val contexts: ArrayDeque<MutableMap<Type, String>> = ArrayDeque()

    public fun isLocal(variable: String): Boolean {
        return variable in this.locals.last()
    }

    public fun contextExists(type: Type): Boolean {
        return this.contexts.findLast { type in it } != null
    }

    public fun inGlobalScope(): Boolean {
        return this.stack.isEmpty()
    }

    @OptIn(ExperimentalContracts::class)
    public inline fun withNewScope(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        this.stack.addLast(mutableMapOf())
        this.locals.addLast(mutableMapOf())
        this.contexts.addLast(mutableMapOf())

        block()

        this.stack.removeLast()
        this.locals.removeLast()
        this.contexts.removeLast()
    }

    @OptIn(ExperimentalContracts::class)
    public inline fun withNestedScope(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        this.stack.addLast(mutableMapOf())
        this.locals.addLast(this.locals.lastOrNull()?.toMutableMap() ?: mutableMapOf())
        this.contexts.addLast(this.contexts.lastOrNull()?.toMutableMap() ?: mutableMapOf())

        block()

        this.stack.removeLast()
        this.locals.removeLast()
        this.contexts.removeLast()
    }

    public fun addVariable(variable: String): Int {
        val currStack = this.stack.last()
        val currLocals = this.locals.last()

        val localIndex = currLocals.size

        currStack[variable] = localIndex
        currLocals[variable] = this.stack.size - 1

        return localIndex
    }

    public fun getVariable(variable: String): Int {
        val currLocals= this.locals.last()
        val index = currLocals[variable] ?: error("unknown variable (should be unreachable)")

        return this.stack[index][variable] ?: error("unknown variable (should be unreachable)")
    }

    public fun addContextVariable(type: Type): Int {
        val currStack = this.stack.last()
        val currContexts = this.contexts.last()
        val currLocals = this.locals.last()

        val localIndex = currLocals.size

        val name = "context_receiver_${type}_${this.stack.size}"

        currStack[name] = localIndex
        currLocals[name] = this.stack.size - 1
        currContexts[type] = name

        return localIndex
    }

    public fun getContextVariable(type: Type): Int {
        val currContexts = this.contexts.last()
        val currLocals = this.locals.last()
        val name = currContexts[type] ?: error("unknown context variable (should be unreachable)")
        val index = currLocals[name] ?: error("unknown context variable with serialized name of $name (should be unreachable")

        return this.stack[index][name] ?: error("unknown context variable with serialized name of $name (should be unreachable")
    }
}