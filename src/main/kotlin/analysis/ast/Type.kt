package analysis.ast

public sealed interface Type {
    // public val isNullable: Boolean // todo add nullable types later
}

// todo: handle generics
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
        public val arity: Int = this.parameterTypes.size
        override fun toString(): String {
            return "(${this.parameterTypes.joinToString(separator = ", ")}) -> ${this.returnType}"
        }
    }

    public val overloads: Set<Overload>
        get() = this.mutableOverloads

    public fun addOverload(parameterTypes: List<Type>, returnType: Type): Overload {
        return Overload(parameterTypes, returnType).also {
            if (it in this.mutableOverloads) {
                error("Overload for function ${this.name} with type $it already exists")
            }
            this.mutableOverloads.add(it)
        }
    }

    override fun toString(): String {
        return "{ ${this.overloads.joinToString(", ")} }"
    }
}

public data class ClassType(
    public val name: String,
    public val superclass: ClassType?,
    public val interfaces: List<InterfaceType>,
    private val mutableProperties: MutableMap<String, Property>,
    private val mutableFunctions: MutableMap<String, Function>,
) {
    public val properties: Map<String, Property>
        get() = this.mutableProperties

    public val functions: Map<String, Function>
        get() = this.mutableFunctions

    public data class Property(val name: String, val type: Type)
    public data class Function(val name: String, val functionType: FunctionType)

    public fun addProperty(name: String, type: Type): Property {
        if (this.mutableProperties.containsKey(name)) {
            error("A property with name $name already exists inside of class ${this.name}")
        }
        return Property(name, type).also {
            this.mutableProperties[name] = it
        }
    }

    public fun addFunction(name: String, parameterTypes: List<Type>, returnType: Type): Function {
        return if (this.mutableFunctions.containsKey(name)) {
            this.mutableFunctions[name]!!.also {
                it.functionType.addOverload(parameterTypes, returnType)
            }
        } else {
            Function(name, FunctionType(name, mutableSetOf(FunctionType.Overload(parameterTypes, returnType)))).also {
                this.mutableFunctions[name] = it
            }
        }
    }
}

// TODO
public data object InterfaceType