package parser.ast

public sealed interface Type

public data class TConstructor(val name: String, val generics: List<Type> = listOf()) : Type {
    override fun toString(): String = if (this.generics.isEmpty()) this.name else "${this.name}<${
        this.generics.joinToString(
            separator = ", ",
            prefix = "",
            postfix = ""
        )
    }>"
}

public data class LambdaTypeConstructor(val contextTypes: List<Type>, val parameterTypes: List<Type>, val returnType: Type) : Type {
    override fun toString(): String = "${if (contextTypes.isNotEmpty()) "context(${contextTypes.joinToString(", ")}) " else ""}(${parameterTypes.joinToString(", ")}) -> $returnType"
}
