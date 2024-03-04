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
                                        Opcode.DoubleSubtract
                                    }
                                    "Int" -> {
                                        Opcode.IntSubtract
                                    }
                                    else -> {
                                        error("invalid unary operator type") // should be unreachable
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
                                        error("invalid unary operator type") // should be unreachable
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
                                        error("invalid unary operator type") // should be unreachable
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
            NullLiteral -> {
                this.currentChunk.write(Opcode.Null.toInt(), this.line++)
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
                    else -> error("invalid unary operator")
                }.toInt(), this.line++)
            }
        }
    }
}