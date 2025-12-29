package analysis.ast

import lexer.Token
import parser.ast.*
import kotlin.text.prependIndent

public sealed interface TypedExpression {
    public val type: Type
}

public sealed interface TypedLiteral<T> : TypedExpression {
    public val value: T
}

@Suppress("UNCHECKED_CAST")
public inline fun <T> TypedLiteral(literal: Literal<T>): TypedLiteral<T> = when(literal) {
    is NullLiteral -> TypedNullLiteral
    is IntLiteral -> TypedIntLiteral(literal.value)
    is DoubleLiteral -> TypedDoubleLiteral(literal.value)
    is BooleanLiteral -> TypedBooleanLiteral(literal.value)
    is StringLiteral -> TypedStringLiteral(literal.value)
} as TypedLiteral<T>

public data class TypedIntLiteral(override val value: Int) : TypedLiteral<Int> {
    override val type: Type
        get() = VariableType("Int")

    override fun toString(): String {
        return "${this.value}<Int>"
    }
}

public data class TypedDoubleLiteral(override val value: Double) : TypedLiteral<Double> {
    override val type: Type
        get() = VariableType("Double")

    override fun toString(): String {
        return "${this.value}<Double>"
    }
}

public data class TypedBooleanLiteral(override val value: Boolean) : TypedLiteral<Boolean> {
    override val type: Type
        get() = VariableType("Boolean")

    override fun toString(): String {
        return "${this.value}<Boolean>"
    }
}

public data object TypedNullLiteral : TypedLiteral<Nothing?> {
    override val value: Nothing?
        get() = null

    override val type: Type
        get() = VariableType("Nothing?")

    override fun toString(): String {
        return "null<Nothing?>"
    }
}

public data class TypedStringLiteral(override val value: String) : TypedLiteral<String> {
    override val type: Type
        get() = VariableType("String")

    override fun toString(): String {
        return "\"${this.value}\"<String>"
    }
}

public data class TypedAssign(val name: Token, val expression: TypedExpression) : TypedExpression {
    override val type: Type
        get() = this.expression.type

    override fun toString(): String {
        return "${this.name.lexeme} = ${this.expression}"
    }
}

public data class TypedBinary(val left: TypedExpression, val operator: Token, val right: TypedExpression, override val type: Type) : TypedExpression {
    override fun toString(): String {
        return "${this.left} ${this.operator.lexeme} ${this.right}<<${this.type}>>"
    }
}

public data class TypedCall(
    val callee: TypedExpression,
    val paren: Token,
    val arguments: List<TypedExpression>,
    override val type: Type,
) : TypedExpression {
    override fun toString(): String {
        return "${this.callee}(${this.arguments.joinToString(", ")})<${this.type}>"
    }
}

public data class TypedInlineCall(
    val callee: TypedExpression,
    val paren: Token,
    val arguments: List<TypedExpression>,
    override val type: Type,
    val inlinedBody: List<TypedStatement>,
    val inlinedParameterNames: List<TypedParameter>,
    val inlinedContexts: List<Pair<Type, Boolean>>,
) : TypedExpression {
    override fun toString(): String {
        return "${this.callee}<inlined>(${this.arguments.joinToString(", ")})<${this.type}>"
    }
}

public data class TypedGet(val instance: TypedExpression, val name: Token, override val type: Type) : TypedExpression {
    override fun toString(): String {
        return "${this.instance}.${this.name.lexeme}<${this.type}>"
    }
}

public data class TypedGrouping(val expression: TypedExpression) : TypedExpression {
    override val type: Type
        get() = this.expression.type

    override fun toString(): String {
        return "(${this.expression})<${this.type}>"
    }
}

public data class TypedIfExpression(
    val condition: TypedExpression,
    val trueBranch: List<TypedStatement>,
    val falseBranch: List<TypedStatement>,
    override val type: Type,
) : TypedExpression {
    override fun toString(): String {
        return "if (${this.condition}) {\n${this.trueBranch.joinToString("\n")}\n} else {\n${this.falseBranch.joinToString("\n")}\n}"
    }
}

public data class TypedLambda(
    val contexts: List<Type>,
    val parameters: List<TypedParameter>,
    val body: List<TypedStatement>,
    override val type: Type,
) : TypedExpression {
    public data class TypedParameter(val name: Token, val type: Type) {
        override fun toString(): String = "${this.name.lexeme}: ${this.type}"
    }

    override fun toString(): String {
        return "{ ${if (this.contexts.isNotEmpty()) "context(${this.contexts.joinToString(", ")}) " else ""}${if (this.parameters.isNotEmpty()) "(${this.parameters.joinToString(", ")}) " else ""}->\n${this.body.joinToString("\n").prependIndent()}\n}"
    }
}

public data class TypedLogical(val left: TypedExpression, val operator: Token, val right: TypedExpression) : TypedExpression {
    override val type: Type
        get() = VariableType("Boolean")

    override fun toString(): String {
        return "${this.left}<${this.left.type}> ${this.operator.lexeme} ${this.right}<${this.right.type}><<${this.type}>>"
    }
}

public data class TypedThis(val keyword: Token, val at: Token?, val label: Type?, override val type: Type) : TypedExpression {
    override fun toString(): String {
        return "this<${this.type}>"
    }
}

public data class TypedUnary(val operator: Token, val expression: TypedExpression, override val type: Type) : TypedExpression {
    override fun toString(): String {
        return "${this.operator.lexeme}${this.expression}<<${this.type}>>"
    }
}

public data class TypedVariable(val name: Token, override val type: Type, val mangledName: String = name.lexeme) : TypedExpression {
    override fun toString(): String {
        return "${this.name.lexeme}<${this.type}>"
    }
}

public data class TypedContextVariable(val depth: Int, override val type: Type) : TypedExpression {
    override fun toString(): String {
        return "context_variable_${type}_$depth"
    }
}