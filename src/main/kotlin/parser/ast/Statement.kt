package parser.ast

public sealed interface Statement

public data class ExpressionStatement(public val expression: Expression) : Statement