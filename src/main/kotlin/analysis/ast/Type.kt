package analysis.ast

public sealed interface Type {
    // public val isNullable: Boolean // todo add nullable types later

    public val mangledName: String
}

// todo: handle generics
public data class VariableType(public val name: String) : Type {
    override val mangledName: String
        get() = this.name

    override fun toString(): String {
        return this.name
    }
}

public data class LambdaType(val contextTypes: List<Type>, val parameterTypes: List<Type>, val returnType: Type) : Type {
    override val mangledName: String
        get() = "Lambda<${(this.contextTypes + this.parameterTypes).let { if (it.isNotEmpty()) it.joinToString(", ", postfix = ", ") { it.mangledName } else ""}}${returnType.mangledName}>"

    override fun toString(): String = "${if (contextTypes.isNotEmpty()) "context(${contextTypes.joinToString(", ")}) " else ""}(${parameterTypes.joinToString(", ")}) -> $returnType"
}

public data class FunctionType(
    public val name: String,
    private val mutableOverloads: MutableSet<Overload> = mutableSetOf()
) : Type {
    public data class Overload(
        public val contextTypes: List<Type>,
        public val parameterTypes: List<Type>,
        public val returnType: Type,
        public val isDeleted: Boolean,
        public val deletionReason: TypedExpression?,
    ) {
        public val arity: Int = this.parameterTypes.size

        public fun overloadSuffix(): String = "${this.contextTypes.joinToString("|") { it.mangledName }}/${this.parameterTypes.joinToString("|") { it.mangledName }}/${returnType.mangledName}"

        override fun toString(): String {
            return "${if (this.contextTypes.isNotEmpty()) "context(${this.contextTypes.joinToString(", ")}) " else ""}(${this.parameterTypes.joinToString(separator = ", ")}) -> ${this.returnType}"
        }
    }

    public val overloads: Set<Overload>
        get() = this.mutableOverloads

    public fun addOverload(
        contextTypes: List<Type>,
        parameterTypes: List<Type>,
        returnType: Type,
        isDeleted: Boolean = false,
        deletionReason: TypedExpression? = null,
    ): Overload {
        return Overload(contextTypes, parameterTypes, returnType, isDeleted, deletionReason).also {
            if (it in this.mutableOverloads) {
                error("Overload for function ${this.name} with type $it already exists")
            }
            this.mutableOverloads.add(it)
        }
    }

    override fun toString(): String {
        return "{ ${this.overloads.joinToString(", ")} }"
    }

    override val mangledName: String
        get() = error("function types should not have a mangled name as it depends on the particular overload (should be unreachable)")
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

    public fun addFunction(
        name: String,
        contextTypes: List<Type>,
        parameterTypes: List<Type>,
        returnType: Type,
        isDeleted: Boolean = false,
        deletionReason: TypedExpression? = null
    ): Function {
        return if (this.mutableFunctions.containsKey(name)) {
            this.mutableFunctions[name]!!.also {
                it.functionType.addOverload(contextTypes, parameterTypes, returnType, isDeleted, deletionReason)
            }
        } else {
            Function(name, FunctionType(name, mutableSetOf(FunctionType.Overload(contextTypes, parameterTypes, returnType, isDeleted, deletionReason)))).also {
                this.mutableFunctions[name] = it
            }
        }
    }
}

// TODO
public data object InterfaceType