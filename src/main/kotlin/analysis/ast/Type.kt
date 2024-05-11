package analysis.ast

public sealed interface Type {
    // public val isNullable: Boolean // todo add nullable types later
}

public data class VariableType(public val name: String) : Type {
    override fun toString(): String {
        return this.name
    }
}

public data class FunctionType(
    public val name: String,
    private val mutableOverloads: MutableSet<Overload> = mutableSetOf()
) : Type {
    public data class Overload(public val parameterTypes: List<Type>, public val returnType: Type) {
        val arity: Int = parameterTypes.size
        override fun toString(): String {
            return "(${parameterTypes.joinToString(separator = ", ")}) -> $returnType"
        }
    }

    public val overloads: Set<Overload>
        get() = mutableOverloads

    public fun addOverload(parameterTypes: List<Type>, returnType: Type): Overload {
        return Overload(parameterTypes, returnType).also {
            mutableOverloads.add(it)
        }
    }

    override fun toString(): String {
        return "{ ${overloads.joinToString(", ")} }"
    }
}

public data class ClassType(
    public val name: String,
    public val superclass: ClassType?,
    public val interfaces: List<InterfaceType>,
    public val properties: Map<String, Property>,
    public val functions: Map<String, Function>,
) {
    public data class Property(val name: String, val type: Type)
    public data class Function(val name: String, val functionType: FunctionType)
}

// TODO
public data object InterfaceType