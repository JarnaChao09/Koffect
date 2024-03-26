package codegen

import lexer.TokenType
import parser.ast.*
import runtime.Chunk
import runtime.Opcode
import runtime.toInt
import runtime.toValue

public class CodeGenerator {
    private lateinit var currentChunk: Chunk
    private var line: Int = 1
    public fun generate(ast: List<Statement>): Chunk {
        this.currentChunk = Chunk()

        this.generateStatements(ast)

        this.currentChunk.write(Opcode.Return.toInt(), this.line++)

        return this.currentChunk
    }

    private fun generateStatements(ast: List<Statement>) {
        ast.forEach {
            when(it) {
                is ExpressionStatement -> {
                    dfs(it.expression)
                }
                is IfStatement -> {
                    this.generateIf(it.condition, it.trueBranch, it.falseBranch)
                }
                is VariableStatement -> {
                    val binding = this.currentChunk.addConstant(it.name.lexeme.toValue())

                    it.initializer?.let { expr ->
                        dfs(expr)
                    } ?: this.currentChunk.write(Opcode.Null.toInt(), this.line++)

                    this.currentChunk.write(Opcode.DefineGlobal.toInt(), this.line)
                    this.currentChunk.write(binding, this.line++)
                }
                is WhileStatement -> {
                    val loopStart = this.currentChunk.code.size

                    this.dfs(it.condition)

                    val exitJump = this.currentChunk.emitJump(Opcode.JumpIfFalse)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                    this.generateStatements(it.body)

                    this.currentChunk.patchLoop(loopStart)

                    this.currentChunk.patchJump(exitJump)
                    this.currentChunk.write(Opcode.Pop.toInt(), this.line++)
                }
            }
        }
    }

    private fun dfs(root: Expression) {
        when (root) {
            is Assign -> {
                val binding = this.currentChunk.addConstant(root.name.lexeme.toValue())

                dfs(root.expression)

                this.currentChunk.write(Opcode.SetGlobal.toInt(), this.line)
                this.currentChunk.write(binding, this.line++)
            }
            is Binary -> {
                dfs(root.left)
                dfs(root.right)

                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.MINUS -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.STAR -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.SLASH -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.MOD -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.EQUALS -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.NOT_EQ -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.GE -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.LE -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.GT -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.LT -> {
                        when (val type = root.left.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    else -> error("invalid binary operator")
                }.toInt(), this.line++)
            }
            is Call -> {
                dfs(root.callee)

                val argCount = root.arguments.size
                root.arguments.forEach(::dfs)

                this.currentChunk.write(Opcode.Call.toInt(), this.line)
                this.currentChunk.write(argCount, this.line++)
            }
            is Grouping -> {
                dfs(root.expression)
            }
            is IfExpression -> {
                this.generateIf(root.condition, root.trueBranch, root.falseBranch)
            }
            is DoubleLiteral -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.DoubleConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is IntLiteral -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.IntConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is BooleanLiteral -> {
                this.currentChunk.write(when (root.value) {
                    true -> Opcode.True
                    false -> Opcode.False
                }.toInt(), this.line++)
            }
            NullLiteral -> {
                this.currentChunk.write(Opcode.Null.toInt(), this.line++)
            }
            is ObjectLiteral<*> -> {
                val constant = this.currentChunk.addConstant(root.value.toValue())
                this.currentChunk.write(Opcode.ObjectConstant.toInt(), this.line)
                this.currentChunk.write(constant, this.line++)
            }
            is Logical -> {
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
            is Unary -> {
                dfs(root.expression)

                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.MINUS -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
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
                        }
                    }
                    TokenType.NOT -> {
                        when (val type = root.type ?: error("Type must be annotated")) {
                            is TConstructor -> {
                                when (type.name) {
                                    "Boolean" -> {
                                        Opcode.Not
                                    }
                                    else -> {
                                        error("Invlaid unary operator type") // should be unreachable
                                    }
                                }
                            }
                        }
                    }
                    else -> error("invalid unary operator")
                }.toInt(), this.line++)
            }
            is Variable -> {
                val binding = this.currentChunk.addConstant(root.name.lexeme.toValue())

                this.currentChunk.write(Opcode.GetGlobal.toInt(), this.line)
                this.currentChunk.write(binding, this.line++)
            }
        }
    }

    private fun generateIf(condition: Expression, trueBranch: List<Statement>, falseBranch: List<Statement>) {
        this.dfs(condition)

        val elseBranch = this.currentChunk.emitJump(Opcode.JumpIfFalse)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.generateStatements(trueBranch)

        val skipElseBranch = this.currentChunk.emitJump(Opcode.Jump)
        this.currentChunk.patchJump(elseBranch)
        this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

        this.generateStatements(falseBranch)

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