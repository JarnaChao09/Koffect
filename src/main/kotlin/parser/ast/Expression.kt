package parser.ast

import lexer.Token

public sealed interface Expression

public sealed interface Literal<T> : Expression {
    public val value: T
}

public inline fun <T : Any> Literal(literal: T?): Literal<T> = when(literal) {
    null -> NullLiteral
    is Int -> IntLiteral(literal)
    is Double -> DoubleLiteral(literal)
    else -> error("Invalid literal type")
} as Literal<T>

public data class IntLiteral(override val value: Int) : Literal<Int>

public data class DoubleLiteral(override val value: Double) : Literal<Double>

public data object NullLiteral : Literal<Nothing?> {
    override val value: Nothing?
        get() = null
}

public data class Binary(val left: Expression, val operator: Token, val right: Expression) : Expression

public data class Unary(val operator: Token, val expression: Expression) : Expression

public data class Grouping(val expression: Expression) : Expression