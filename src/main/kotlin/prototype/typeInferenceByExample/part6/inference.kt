package prototype.typeInferenceByExample.part6

public sealed interface Type

public data class TConstructor(val name: String, val generics: List<Type> = listOf()) : Type {
    override fun toString(): String = if (this.generics.isEmpty()) this.name else "${this.name}<${this.generics.joinToString(separator = ", ", prefix = "", postfix = "")}>"
}

public data class TVariable(val index: Int) : Type

public sealed interface Expr

public data class ELambda(val parameters: List<Parameter>, val returnType: Type?, val body: Expr) : Expr

public data class EApply(val function: Expr, val arguments: List<Expr>) : Expr

public data class EVariable(val name: String) : Expr

public data class ELet(val name: String, val typeAnnotation: Type?, val value: Expr, val body: Expr) : Expr

public data class EInt(val value: Int) : Expr

public data class EString(val value: String) : Expr

public data class EArray(val itemType: Type?, val items: List<Expr>) : Expr

public data class Parameter(val name: String, val typeAnnotation: Type?)

public sealed interface Constraint

public data class CEquality(val t1: Type, val t2: Type) : Constraint

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
                    require(t1.name == t2.name)
                    require(t1.generics.size == t2.generics.size)
                    t1.generics.zip(t2.generics).forEach {
                        unify(it.first, it.second)
                    }
                }

                is TVariable -> {
                    if (substitution[t2.index] != t2) {
                        unify(t1, substitution[t2.index])
                    } else {
                        require(!occursIn(t2.index, t1))
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
                        require(!occursIn(t1.index, t2))
                        substitution[t1.index] = t2
                    }
                }

                is TConstructor -> {
                    if (substitution[t1.index] != t1) {
                        unify(substitution[t1.index], t2)
                    } else {
                        require(!occursIn(t1.index, t2))
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

public fun infer(environment: Map<String, Type>, expectedType: Type, expression: Expr): Expr = when (expression) {
    is EApply -> {
        val argumentTypes = expression.arguments.map {
            freshTypeVariable()
        }
        val functionType = TConstructor("Function${expression.arguments.size}", argumentTypes + listOf(expectedType))
        val newFunction = infer(environment, functionType, expression.function)
        val newArguments = expression.arguments.zip(argumentTypes).map {
            infer(environment, it.second, it.first)
        }
        EApply(newFunction, newArguments)
    }
    is EArray -> {
        val newItemType = expression.itemType ?: freshTypeVariable()
        val newItems = expression.items.map {
            infer(environment, newItemType, it)
        }
        typeConstraints += CEquality(expectedType, TConstructor("Array", listOf(newItemType)))
        EArray(newItemType, newItems)
    }
    is EInt -> {
        expression.also {
            typeConstraints += CEquality(expectedType, TConstructor("Int"))
        }
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
        val newBody = infer(newEnvironment, newReturnType, expression.body)
        typeConstraints += CEquality(expectedType, TConstructor("Function${expression.parameters.size}", newParameterTypes + listOf(newReturnType)))
        ELambda(newParameters, newReturnType, newBody)
    }
    is ELet -> {
        val newTypeAnnotation = expression.typeAnnotation ?: freshTypeVariable()
        val newValue = infer(environment, newTypeAnnotation, expression.value)
        val newEnvironment = environment + (expression.name to newTypeAnnotation)
        val newBody = infer(newEnvironment, expectedType, expression.body)
        ELet(expression.name, newTypeAnnotation, newValue, newBody)
    }
    is EString -> {
        expression.also {
            typeConstraints += CEquality(expectedType, TConstructor("String"))
        }
    }
    is EVariable -> {
        expression.also {
            typeConstraints += CEquality(expectedType, environment[it.name]!!)
        }
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
        val newBody = substituteExpression(expression.body)
        ELet(expression.name, newTypeAnnotation, newValue, newBody)
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
            TConstructor("Function2", listOf(
                TConstructor("Int"),
                TConstructor("Int"),
                TConstructor("Int"),
            ))
        )
    }
}

public fun infer(expression: Expr): Expr {
    val newExpr = infer(emptyMap(), freshTypeVariable(), expression)
    solveConstraints()
    return substituteExpression(newExpr)
}

public fun main() {
    println(
        infer(
            ELet(
                "singleton",
                null,
                ELambda(
                    listOf(Parameter("x", null)),
                    null,
                    EArray(null, listOf(EVariable("x"))),
                ),
                EApply(
                    EVariable("singleton"),
                    listOf(EInt(42))
                ),
            )
        )
    )
}
/*
ELet(     singleton,           Some(Function1<Int, Array<Int>>),      ELambda(       List(Parameter(     x,           Some(Int))),      Some(Array<Int>),     EArray(    Some(Int),  List(EVariable(     x)))),      EApply(         EVariable(     singleton),       List(EInt(42))))
ELet(name=singleton, typeAnnotation=Function1<Int, Array<Int>>, value=ELambda(parameters=[Parameter(name=x, typeAnnotation=Int)], returnType=Array<Int>, body=EArray(itemType=Int, items=[EVariable(name=x)])), body=EApply(function=EVariable(name=singleton), arguments=[EInt(value=42)]))
ELet(name=singleton, typeAnnotation=TConstructor(name=Function1, generics=[TConstructor(name=Int, generics=[]), TConstructor(name=Array, generics=[TConstructor(name=Int, generics=[])])]), value=ELambda(parameters=[Parameter(name=x, typeAnnotation=TConstructor(name=Int, generics=[]))], returnType=TConstructor(name=Array, generics=[TConstructor(name=Int, generics=[])]), body=EArray(itemType=TConstructor(name=Int, generics=[]), items=[EVariable(name=x)])), body=EApply(function=EVariable(name=singleton), arguments=[EInt(value=42)]))
 */