package analysis

import analysis.ast.*

public class Environment(
    private val variables: MutableMap<String, Type> = mutableMapOf(),
    private val klasses: MutableMap<String, ClassType> = mutableMapOf(),
    private val contextVariables: MutableMap<Type, String> = mutableMapOf(),
    public val enclosing: Environment? = null,
    private val depth: Int = 0,
) {
    public constructor(enclosing: Environment): this(mutableMapOf(), mutableMapOf(), mutableMapOf(), enclosing, enclosing.depth + 1)

    public fun addVariable(name: String, type: Type) {
        if (this.variables.containsKey(name)) {
            error("Variable with name '$name' already exists")
        }

        this.variables[name] = type
    }

    public fun getVariable(name: String): Type? {
        return this.variables.getOrElse(name) {
            this.enclosing?.getVariable(name)
        }
    }

    // public operator fun plusAssign(variable: Pair<String, Type>) {
    //     this.addVariable(variable.first, variable.second)
    // }

    public fun addClass(klassName: String, type: ClassType) {
        if (this.klasses.containsKey(klassName)) {
            error("Class with name '$klassName' already exists")
        }

        this.klasses[klassName] = type
    }

    public fun getClass(name: String): ClassType? {
        return this.klasses.getOrElse(name) {
            this.enclosing?.getClass(name)
        }
    }

    // public operator fun plusAssign(klass: Pair<String, ClassType>) {
    //     this.addClass(klass.first, klass.second)
    // }

    public fun addContextVariable(type: Type) {
        if (this.contextVariables.containsKey(type)) {
            error("Context variable with type '$type' already exists")
        }

        this.contextVariables[type] = "context_variable_${type}_$depth"
    }

    public fun getContextVariable(type: Type): String? {
        return this.contextVariables.getOrElse(type) {
            this.enclosing?.getContextVariable(type)
        }
    }
}

public fun buildEnvironment(block: EnvironmentBuilder.() -> Unit): Environment {
    val builder = EnvironmentBuilder()
    builder.block()
    return builder.build()
}

@DslMarker
public annotation class EnvironmentDSL

@EnvironmentDSL
public class EnvironmentBuilder {
    private val variables: MutableMap<String, Type> = mutableMapOf()
    private val klasses: MutableMap<String, ClassType> = mutableMapOf()

    public fun variable(variable: String, type: String) {
        require(variable !in this.variables) {
            "Variable with name '$variable' already exists"
        }

        this.variables[variable] = VariableType(type)
    }

    public fun function(function: String, contexts: List<String> = emptyList(), block: FunctionBuilder.() -> Unit) {
        require(function !in this.variables) {
            "Function with name '$function' already exists. All overloads for a function must be in the same block"
        }

        val builder = FunctionBuilder(function, contexts = contexts.map { VariableType(it) })
        builder.block()
        this.variables[function] = builder.build()
    }

    public operator fun String.invoke(block: ClassBuilder.() -> Unit) {
        require(this !in this@EnvironmentBuilder.klasses) {
            "Class with name '$this' already exists"
        }

        val builder = ClassBuilder(this)
        builder.block()
        this@EnvironmentBuilder.klasses[this] = builder.build(this@EnvironmentBuilder.klasses)
    }

    public fun build(): Environment {
        return Environment(
            variables = this.variables,
            klasses = this.klasses,
        )
    }
}

@EnvironmentDSL
public class FunctionBuilder(private val function: String, private val contexts: List<Type>) {
    private val overloads: MutableSet<FunctionType.Overload> = mutableSetOf()

    public infix fun List<String>.returns(returnType: String) {
        // todo: assuming all are variable types for now, double check if this assumption holds true
        this@FunctionBuilder.overloads += FunctionType.Overload(contexts, this.map { VariableType(it) }, VariableType(returnType))
    }

    public fun build(): FunctionType {
        require(overloads.isNotEmpty()) {
            "A function overload must be given for the function $function"
        }

        return FunctionType(function, overloads)
    }
}

@EnvironmentDSL
public class ClassBuilder(private val klass: String) {
    public var superClass: String? = null
    private val interfaces: MutableList<String> = mutableListOf() // todo: update once interfaces are added
    private val properties: MutableMap<String, ClassType.Property> = mutableMapOf()
    private val functions: MutableMap<String, ClassType.Function> = mutableMapOf()

    public fun property(property: String, type: String) {
        require(property !in this.properties) {
            "Property with name '$property' already exists"
        }

        this.properties[property] = ClassType.Property(property, VariableType(type))
    }

    public fun function(function: String, contexts: List<String> = emptyList(), block: FunctionBuilder.() -> Unit) {
        require(function !in this.functions) {
            "Function with name '$function' already exists. All overloads for a function must be in the same block"
        }

        val builder = FunctionBuilder(function, contexts = contexts.map { VariableType(it) })
        builder.block()
        this.functions[function] = ClassType.Function(function, builder.build())
    }

    public fun build(currentClasses: Map<String, ClassType>): ClassType {
        // val superClassType = currentClasses[this.superClass] ?: error("Unknown Super Class ${this.superClass}")

        return ClassType(
            klass,
            null,
            emptyList(),
            this.properties,
            this.functions,
        )
    }
}