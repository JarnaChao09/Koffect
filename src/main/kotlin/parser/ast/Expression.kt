package parser.ast

import lexer.Token

public sealed interface Expression {
    public var type: Type?
}

public sealed interface Literal<T> : Expression {
    public val value: T
}

@Suppress("UNCHECKED_CAST")
public inline fun <T : Any> Literal(literal: T?): Literal<T> = when(literal) {
    null -> NullLiteral
    is Int -> IntLiteral(literal)
    is Double -> DoubleLiteral(literal)
    is Boolean -> BooleanLiteral(literal)
    else -> error("Invalid literal type")
} as Literal<T>

public data class IntLiteral(override val value: Int) : Literal<Int> {
    override var type: Type?
        get() = TConstructor("Int")
        set(_) = error("Cannot re-set the type of an Int Literal")
}

public data class DoubleLiteral(override val value: Double) : Literal<Double> {
    override var type: Type?
        get() = TConstructor("Double")
        set(_) = error("Cannot re-set the type of a Double Literal")
}

public data class BooleanLiteral(override val value: Boolean) : Literal<Boolean> {
    override var type: Type?
        get() = TConstructor("Boolean")
        set(_) = error("Cannot re-set the type of a Boolean Literal")
}

public data object NullLiteral : Literal<Nothing?> {
    override val value: Nothing?
        get() = null

    override var type: Type?
        get() = TConstructor("Nothing?")
        set(_) = error("Cannot re-set the type of a Null Literal")
}

public data class Binary(val left: Expression, val operator: Token, val right: Expression, override var type: Type? = null) : Expression

public data class Unary(val operator: Token, val expression: Expression, override var type: Type? = null) : Expression

public data class Grouping(val expression: Expression, override var type: Type? = null) : Expression

public data class Logical(val left: Expression, val operator: Token, val right: Expression) : Expression {
    override var type: Type?
        get() = TConstructor("Boolean")
        set(_) = error("Logical operators must always return Boolean")
}

public data class If(
    val condition: Expression,
    val trueBranch: Expression,
    val falseBranch: Expression,
    override var type: Type? = null,
) : Expression