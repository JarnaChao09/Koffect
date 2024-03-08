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

        ast.forEach {
            when(it) {
                is ExpressionStatement -> {
                    dfs(it.expression)
                }
            }
        }

        this.currentChunk.write(Opcode.Return.toInt(), this.line++)

        return this.currentChunk
    }

    private fun dfs(root: Expression) {
        when (root) {
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
            is Grouping -> {
                dfs(root.expression)
            }
            is If -> {
                dfs(root.condition)

                val elseBranch = this.currentChunk.emitJump(Opcode.JumpIfFalse)
                this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                dfs(root.trueBranch)

                val skipElseBranch = this.currentChunk.emitJump(Opcode.Jump)
                this.currentChunk.patchJump(elseBranch)
                this.currentChunk.write(Opcode.Pop.toInt(), this.line++)

                dfs(root.falseBranch)

                this.currentChunk.patchJump(skipElseBranch)
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
        }
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
}