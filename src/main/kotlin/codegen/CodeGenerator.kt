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

    public fun generate(ast: List<TypedStatement>): Chunk {
        this.currentChunk = Chunk()

        this.generateStatements(ast)

        this.currentChunk.write(Opcode.Null.toInt(), this.line++)
        this.currentChunk.write(Opcode.Return.toInt(), this.line++)

        return this.currentChunk
    }

    private fun generateStatements(ast: List<TypedStatement>) {
        ast.forEach {
            when(it) {
                is TypedClassDeclaration -> {
                    TODO()
                }
                is TypedExpressionStatement -> {
                    dfs(it.expression)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                }
                is TypedReturnExpressionStatement -> {
                    dfs(it.returnExpression)
                }
                is TypedIfStatement -> {
                    this.generateIf(it.condition, it.trueBranch, it.falseBranch)
                }
                is TypedFunctionDeclaration -> {
                    val binding = this.currentChunk.addConstant(it.name.lexeme.toValue())

                    val oldChunk = this.currentChunk

                    this.currentChunk = Chunk()
                    this.returnEmitted = false

                    val function: ObjectFunction

                    this.stack.withNewScope {
                        it.parameters.forEach { parameter ->
                            this.stack.addVariable(parameter.name.lexeme)
                        }

                        this.generateStatements(it.body)

                        if (!returnEmitted) {
                            val constant = this.currentChunk.addConstant(UnitValue)

                            this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                            this.currentChunk.write(constant, this.line++)

                            this.currentChunk.write(Opcode.Return.toInt(), this.line++)
                        }

                        function = ObjectFunction(Function(it.name.lexeme, it.arity, this.currentChunk))

                        this.currentChunk = oldChunk
                    }

                    val constant = this.currentChunk.addConstant(function)
                    this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                    this.currentChunk.write(constant, this.line++)

                    this.currentChunk.write(Opcode.DefineGlobal.toInt(), this.line)
                    this.currentChunk.write(binding, this.line++)
                }
                is TypedVariableStatement -> {
                    it.initializer?.let { expr ->
                        dfs(expr)
                    } ?: this.currentChunk.write(Opcode.Null.toInt(), this.line++)

                    if (this.stack.inGlobalScope()) {
                        val binding = this.currentChunk.addConstant(it.name.lexeme.toValue())

                        this.currentChunk.write(Opcode.DefineGlobal.toInt(), this.line)
                        this.currentChunk.write(binding, this.line++)
                    } else {
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

                    this.dfs(it.condition)

                    val exitJump = this.currentChunk.emitJump(Opcode.JumpIfFalse)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                    this.stack.withNestedScope {
                        this.generateStatements(it.body)
                    }

                    this.currentChunk.patchLoop(loopStart)

                    this.currentChunk.patchJump(exitJump)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                }
                is TypedReturnStatement -> {
                    this.returnEmitted = true

                    it.value?.let { returnValue ->
                        this.dfs(returnValue)
                    } ?: run {
                        val constant = this.currentChunk.addConstant(UnitValue)
                        this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                        this.currentChunk.write(constant, this.line)
                    }

                    this.currentChunk.write(Opcode.Return.toInt(), this.line++)
                }
            }
        }
    }

    private fun dfs(root: TypedExpression) {
        when (root) {
            is TypedAssign -> {
                dfs(root.expression)

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
                dfs(root.left)
                dfs(root.right)

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
                dfs(root.callee)

                val argCount = root.arguments.size
                root.arguments.forEach(::dfs)

                this.currentChunk.write(Opcode.Call.toInt(), this.line)
                this.currentChunk.write(argCount, this.line++)
            }
            is TypedGet -> {
                TODO()
            }
            is TypedGrouping -> {
                dfs(root.expression)
            }
            is TypedIfExpression -> {
                this.generateIf(root.condition, root.trueBranch, root.falseBranch)
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
            is TypedLogical -> {
                dfs(root.left)

                val jumpType = when (root.operator.type) {
                    TokenType.AND -> Opcode.JumpIfFalse
                    TokenType.OR -> Opcode.JumpIfTrue
                    else -> error("Invalid Logical Operator") // should be unreachable
                }

                val jump = this.currentChunk.emitJump(jumpType)

                this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                dfs(root.right)

                this.currentChunk.patchJump(jump)
            }
            is TypedThis -> {
                TODO()
            }
            is TypedUnary -> {
                dfs(root.expression)

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
                if (this.stack.inGlobalScope() || !this.stack.isLocal(root.name.lexeme)) {
                    val binding = this.currentChunk.addConstant(root.name.lexeme.toValue())

                    this.currentChunk.write(Opcode.GetGlobal.toInt(), this.line)
                    this.currentChunk.write(binding, this.line++)
                } else {
                    this.currentChunk.write(Opcode.GetLocal.toInt(), this.line)
                    this.currentChunk.write(
                        this.stack.getVariable(root.name.lexeme),
                        this.line++
                    )
                }
            }
        }
    }

    private fun generateIf(condition: TypedExpression, trueBranch: List<TypedStatement>, falseBranch: List<TypedStatement>) {
        this.dfs(condition)

        val elseBranch = this.currentChunk.emitJump(Opcode.JumpIfFalse)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.stack.withNestedScope {
            this.generateStatements(trueBranch)
        }

        val skipElseBranch = this.currentChunk.emitJump(Opcode.Jump)
        this.currentChunk.patchJump(elseBranch)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.stack.withNestedScope {
            this.generateStatements(falseBranch)
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

    public fun isLocal(variable: String): Boolean {
        return variable in this.locals.last()
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

        block()

        this.stack.removeLast()
        this.locals.removeLast()
    }

    @OptIn(ExperimentalContracts::class)
    public inline fun withNestedScope(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        this.stack.addLast(mutableMapOf())
        this.locals.addLast(this.locals.lastOrNull()?.toMutableMap() ?: mutableMapOf())

        block()

        this.stack.removeLast()
        this.locals.removeLast()
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
}