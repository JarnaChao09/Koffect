package parser.ast

import lexer.Token
import lexer.TokenType

public sealed interface Statement

public data class ExpressionStatement(public val expression: Expression) : Statement

public sealed interface VariableStatement : Statement {
    public val name: Token

    public val type: Type?

    public val initializer: Expression?

    public companion object {
        public operator fun invoke(variableType: TokenType, name: Token, type: Token?, init: Expression?): VariableStatement {
            return when (variableType) {
                TokenType.VAL -> Val(name, type?.let { TConstructor(it.lexeme) }, init)
                TokenType.VAR -> Var(name, type?.let { TConstructor(it.lexeme) }, init)
                else -> error("unreachable: should only be triggered with val/var")
            }
        }
    }
}

public data class Var(override val name: Token, override val type: Type?, override val initializer: Expression?) : VariableStatement

public data class Val(override val name: Token, override val type: Type?, override val initializer: Expression?) : VariableStatement