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

                // todo: do correct typed code gen (currently assuming always int)
                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> Opcode.IntAdd
                    TokenType.MINUS -> Opcode.IntSubtract
                    TokenType.STAR -> Opcode.IntMultiply
                    TokenType.SLASH -> Opcode.IntDivide
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

                // todo: do correct typed code gen (currently assuming always int)
                this.currentChunk.write(when (root.operator.type) {
                    TokenType.PLUS -> Opcode.IntIdentity
                    TokenType.MINUS -> Opcode.IntNegate
                    else -> error("invalid unary operator")
                }.toInt(), this.line++)
            }
        }
    }
}