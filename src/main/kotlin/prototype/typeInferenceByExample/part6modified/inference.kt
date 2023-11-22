package prototype.typeInferenceByExample.part6modified

import java.lang.RuntimeException

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

public data class TVariable(val index: Int) : Type

public sealed interface Expr

public data class ELambda(val parameters: List<Parameter>, val returnType: Type?, val body: Expr) : Expr {
    override fun toString(): String = "(${this.parameters.joinToString(", ")}) -> ${this.returnType} { ${this.body} }"
}

public data class EApply(val function: Expr, val arguments: List<Expr>) : Expr {
    override fun toString(): String = "${this.function}(${this.arguments.joinToString(separator = ", ")})"
}

public data class EVariable(val name: String) : Expr {
    override fun toString(): String = this.name
}

public data class ELet(val name: String, val typeAnnotation: Type?, val value: Expr) : Expr {
    override fun toString(): String = "let ${this.name}: ${this.typeAnnotation} = ${this.value}"
}

public data class EInt(val value: Int) : Expr {
    override fun toString(): String = "${this.value}"
}

public data class EString(val value: String) : Expr {
    override fun toString(): String = this.value
}

public data class EArray(val itemType: Type?, val items: List<Expr>) : Expr {
    override fun toString(): String = "Array<${this.itemType}>${this.items.joinToString(", ", prefix = "(", postfix = ")")}"
}

public data class Parameter(val name: String, val typeAnnotation: Type?) {
    override fun toString(): String = "${this.name}: ${this.typeAnnotation}"
}

public sealed interface Constraint

public data class CEquality(val t1: Type, val t2: Type) : Constraint

public data class TypeError(override val message: String?) : RuntimeException()

public val substitution: MutableList<Type> = mutableListOf()

public val typeConstraints: MutableList<Constraint> = mutableListOf()

public fun freshTypeVariable(): TVariable {
    return TVariable(substitution.size).also {
        substitution += it
    }
}

public fun unify(t1: Type, t2: Type) {
    when (t1) {
        is TConstructor -> {
            when (t2) {
                is TConstructor -> {
                    if (t1.name != t2.name || t1.generics.size != t2.generics.size) {
                        throw TypeError("Type Mismatch: ${substitute(t1)} vs ${substitute(t2)}")
                    }
                    t1.generics.zip(t2.generics).forEach {
                        unify(it.first, it.second)
                    }
                }

                is TVariable -> {
                    if (substitution[t2.index] != t2) {
                        unify(t1, substitution[t2.index])
                    } else {
                        if (occursIn(t2.index, t1)) {
                            throw TypeError("Infinite Type: t${t2.index} = ${substitute(t1)}")
                        }
                        substitution[t2.index] = t1
                    }
                }
            }
        }

        is TVariable -> {
            when (t2) {
                is TVariable -> {
                    if (t1.index == t2.index) {
                        // do nothing
                    } else if (substitution[t1.index] != t1) {
                        unify(substitution[t1.index], t2)
                    } else if (substitution[t2.index] != t2) {
                        unify(t1, substitution[t2.index])
                    } else {
                        if (occursIn(t1.index, t2)) {
                            throw TypeError("Infinite Type: t${t1.index} = ${substitute(t2)}")
                        }
                        substitution[t1.index] = t2
                    }
                }

                is TConstructor -> {
                    if (substitution[t1.index] != t1) {
                        unify(substitution[t1.index], t2)
                    } else {
                        if (occursIn(t1.index, t2)) {
                            throw TypeError("Infinite Type: t${t1.index} = ${substitute(t2)}")
                        }
                        substitution[t1.index] = t2
                    }
                }
            }
        }
    }
}

public fun occursIn(index: Int, t: Type): Boolean = when (t) {
    is TVariable -> {
        if (substitution[t.index] != t) {
            occursIn(index, substitution[t.index])
        } else {
            index == t.index
        }
    }

    is TConstructor -> {
        t.generics.any {
            occursIn(index, it)
        }
    }
}

public fun infer(environment: Map<String, Type>, expectedType: Type, expression: Expr): Pair<Expr, Map<String, Type>> = when (expression) {
    is EApply -> {
        val argumentTypes = expression.arguments.map {
            freshTypeVariable()
        }
        val functionType = TConstructor("Function${expression.arguments.size}", argumentTypes + listOf(expectedType))
        val newFunction = infer(environment, functionType, expression.function)
        val newArguments = expression.arguments.zip(argumentTypes).map {
            infer(environment, it.second, it.first).first
        }
        EApply(newFunction.first, newArguments) to environment
    }

    is EArray -> {
        val newItemType = expression.itemType ?: freshTypeVariable()
        val newItems = expression.items.map {
            infer(environment, newItemType, it).first
        }
        typeConstraints += CEquality(expectedType, TConstructor("Array", listOf(newItemType)))
        EArray(newItemType, newItems) to environment
    }

    is EInt -> {
        expression.also {
            typeConstraints += CEquality(expectedType, TConstructor("Int"))
        } to environment
    }

    is ELambda -> {
        val newReturnType = expression.returnType ?: freshTypeVariable()
        val newParameterTypes = expression.parameters.map {
            it.typeAnnotation ?: freshTypeVariable()
        }
        val newParameters = expression.parameters.zip(newParameterTypes).map {
            it.first.copy(typeAnnotation = it.second)
        }
        val newEnvironment = environment + newParameters.map {
            it.name to it.typeAnnotation!!
        }
        val (newBody, newEnv) = infer(newEnvironment, newReturnType, expression.body)
        typeConstraints += CEquality(
            expectedType,
            TConstructor("Function${expression.parameters.size}", newParameterTypes + listOf(newReturnType))
        )
        ELambda(newParameters, newReturnType, newBody) to newEnv
    }

    is ELet -> {
        val newTypeAnnotation = expression.typeAnnotation ?: freshTypeVariable()
        val (newValue, env) = infer(environment, newTypeAnnotation, expression.value)
        val newEnvironment = env + (expression.name to newTypeAnnotation)
        ELet(expression.name, newTypeAnnotation, newValue) to newEnvironment
    }

    is EString -> {
        expression.also {
            typeConstraints += CEquality(expectedType, TConstructor("String"))
        } to environment
    }

    is EVariable -> {
        expression.also {
            typeConstraints += CEquality(expectedType, environment[it.name] ?: throw TypeError("Variable not in scope: ${it.name}"))
        } to environment
    }
}

public fun substituteExpression(expression: Expr): Expr = when (expression) {
    is EApply -> {
        val newFunction = substituteExpression(expression.function)
        val newArguments = expression.arguments.map(::substituteExpression)
        EApply(newFunction, newArguments)
    }

    is EArray -> {
        val newItemType = expression.itemType?.let(::substitute)
        val newItems = expression.items.map(::substituteExpression)
        EArray(newItemType, newItems)
    }

    is EInt -> {
        expression
    }

    is ELambda -> {
        val newReturnType = expression.returnType?.let(::substitute)
        val newParameters = expression.parameters.map {
            it.copy(typeAnnotation = it.typeAnnotation?.let(::substitute))
        }
        val newBody = substituteExpression(expression.body)
        ELambda(newParameters, newReturnType, newBody)
    }

    is ELet -> {
        val newTypeAnnotation = expression.typeAnnotation?.let(::substitute)
        val newValue = substituteExpression(expression.value)
        ELet(expression.name, newTypeAnnotation, newValue)
    }

    is EString -> {
        expression
    }

    is EVariable -> {
        expression
    }
}

public fun solveConstraints() {
    typeConstraints.forEach {
        when (it) {
            is CEquality -> {
                unify(it.t1, it.t2)
            }
        }
    }
    typeConstraints.clear()
}

public fun substitute(t: Type): Type = when (t) {
    is TVariable -> {
        if (substitution[t.index] != t) {
            substitute(substitution[t.index])
        } else {
            t
        }
    }

    is TConstructor -> {
        TConstructor(t.name, t.generics.map { substitute(it) })
    }
}

public val initialEnvironment: Map<String, Type> = buildMap {
    for (i in listOf("+", "-", "*", "/")) {
        put(
            i,
            TConstructor(
                "Function2", listOf(
                    TConstructor("Int"),
                    TConstructor("Int"),
                    TConstructor("Int"),
                )
            )
        )
    }
}

public fun printInfer(expressions: List<Expr>): String {
    return try {
        var currentEnv = initialEnvironment
        val newExprs = expressions.map {
            val (e, env) = infer(currentEnv, freshTypeVariable(), it)
            currentEnv = env
            e
        }
        solveConstraints()
        newExprs.map {
            substituteExpression(it)
        }.joinToString(separator = "\n")
    } catch (typeError: TypeError) {
        typeError.message!!
    }
}

public fun main() {
    println(printInfer(
        listOf(
            ELet(
                "add2",
                null,
                ELambda(
                    listOf(
                        Parameter("x", null),
                        Parameter("y", null)
                    ),
                    null,
                    EApply(
                        EVariable("+"), listOf(
                            EVariable("x"),
                            EVariable("y")
                        )
                    ),
                )
            ),
            ELet(
                "mul2",
                null,
                ELambda(
                    listOf(
                        Parameter("x", null),
                        Parameter("y", null),
                    ),
                    null,
                    EApply(
                        EVariable("*"), listOf(
                            EVariable("x"),
                            EVariable("y"),
                        )
                    )
                )
            ),
            ELet(
                "multadd2",
                null,
                ELambda(
                    listOf(
                        Parameter("x", null),
                    ),
                    null,
                    EApply(
                        EVariable("mul2"), listOf(
                            EVariable("x"),
                            EApply(
                                EVariable("add2"), listOf(
                                    EVariable("x"),
                                    EVariable("x"),
                                ),
                            ),
                        )
                    )
                )
            ),
            EApply(
                EVariable("multadd2"), listOf(
                    EInt(42),
                )
            ),
        ).also { println(it.joinToString(separator = "\n")) }
    ))
}