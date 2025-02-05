package parser.ast

import lexer.Token

public sealed interface Expression

public sealed interface Literal<T> : Expression {
    public val value: T
}

@Suppress("UNCHECKED_CAST")
public inline fun <T : Any> Literal(literal: T?): Literal<T> = when(literal) {
    null -> NullLiteral
    is Int -> IntLiteral(literal)
    is Double -> DoubleLiteral(literal)
    is Boolean -> BooleanLiteral(literal)
    is String -> StringLiteral(literal)
    else -> error("Invalid literal type")
} as Literal<T>

public data class IntLiteral(override val value: Int) : Literal<Int>

public data class DoubleLiteral(override val value: Double) : Literal<Double>

public data class BooleanLiteral(override val value: Boolean) : Literal<Boolean>

public data object NullLiteral : Literal<Nothing?> {
    override val value: Nothing?
        get() = null
}

public data class StringLiteral(override val value: String) : Literal<String>

public data class Assign(val name: Token, val expression: Expression) : Expression

public data class Binary(val left: Expression, val operator: Token, val right: Expression) : Expression

public data class Call(
    val callee: Expression,
    val paren: Token,
    val pinnedContexts: List<Type>,
    val arguments: List<Expression>,
) : Expression

public data class Get(val instance: Expression, val name: Token) : Expression

public data class Grouping(val expression: Expression) : Expression

public data class IfExpression(
    val condition: Expression,
    val trueBranch: List<Statement>,
    val falseBranch: List<Statement>,
) : Expression

public data class Lambda(val contexts: List<Type>, val parameters: List<Parameter>, val body: List<Statement>) : Expression {
    public data class Parameter(val name: Token, val type: Type?)
}

public data class Logical(val left: Expression, val operator: Token, val right: Expression) : Expression

// todo: update label to be valid labels, not just types
public data class This(val keyword: Token, val at: Token? = null, val label: Type? = null) : Expression

public data class Unary(val operator: Token, val expression: Expression) : Expression

public data class Variable(val name: Token) : Expression