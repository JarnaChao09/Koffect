package prototype.typeInferenceByExample.part7

public val genericParameterNames: Iterator<String> = generateSequence('A' to 0) { (c, i) ->
    if (c == 'Z') {
        'A' to (i + 1)
    } else {
        (c + 1) to i
    }
}.map { (c, i) ->
    "$c${if (i == 0) "" else i}"
}.iterator()

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

public data class TVariable(val index: Int) : Type {
    override fun toString(): String = "t${this.index}"
}

public sealed interface Expr

public data class EFunctions(val functions: List<GenericFunction>, val body: Expr) : Expr {
    override fun toString(): String = "${this.functions.joinToString("\n")}\n${this.body}"
}

public data class GenericFunction(val name: String, val typeAnnotation: GenericType?, val body: Expr) {
    override fun toString(): String = "fun ${this.name}${this.typeAnnotation ?: ""} = ${this.body}"
}

public data class GenericType(val generics: List<String>, val uninstantiatedType: Type) {
    override fun toString(): String = "<${this.generics.joinToString(", ")}>: ${this.uninstantiatedType}"
}

public data class ELambda(val parameters: List<Parameter>, val returnType: Type?, val body: Expr) : Expr {
    override fun toString(): String = "(${this.parameters.joinToString(", ")}) -> ${this.returnType} { ${this.body} }"
}

public data class EApply(val function: Expr, val arguments: List<Expr>) : Expr {
    override fun toString(): String = "${this.function}(${this.arguments.joinToString(separator = ", ")})"
}

public data class EVariable(val name: String, val generics: List<Type> = emptyList()) : Expr {
    override fun toString(): String =
        "${this.name}${if (this.generics.isNotEmpty()) "<${this.generics.joinToString(", ")}>" else ""}"
}

public data class ELet(val name: String, val typeAnnotation: Type?, val value: Expr, val body: Expr) : Expr {
    override fun toString(): String = "let ${this.name}: ${this.typeAnnotation} = ${this.value}"
}

public data class EInt(val value: Int) : Expr {
    override fun toString(): String = "Int(${this.value})"
}

public data class EString(val value: String) : Expr {
    override fun toString(): String = "String(${this.value})"
}

public data class EArray(val itemType: Type?, val items: List<Expr>) : Expr {
    override fun toString(): String =
        "Array<${this.itemType}>${this.items.joinToString(", ", prefix = "(", postfix = ")")}"
}

public data class ESemicolon(val before: Expr, val after: Expr) : Expr {
    override fun toString(): String = "${this.before};\n${this.after}"
}

public data class Parameter(val name: String, val typeAnnotation: Type?) {
    override fun toString(): String = "${this.name}: ${this.typeAnnotation}"
}

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

public fun infer(environment: Map<String, GenericType>, expectedType: Type, expression: Expr): Expr =
    when (expression) {
        is EApply -> {
            val argumentTypes = expression.arguments.map {
                freshTypeVariable()
            }
            val functionType =
                TConstructor("Function${expression.arguments.size}", argumentTypes + listOf(expectedType))
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
                it.name to GenericType(emptyList(), it.typeAnnotation!!)
            }
            val newBody = infer(newEnvironment, newReturnType, expression.body)
            typeConstraints += CEquality(
                expectedType,
                TConstructor("Function${expression.parameters.size}", newParameterTypes + listOf(newReturnType))
            )
            ELambda(newParameters, newReturnType, newBody)
        }

        is ELet -> {
            val newTypeAnnotation = expression.typeAnnotation ?: freshTypeVariable()
            val newValue = infer(environment, newTypeAnnotation, expression.value)
            val newEnvironment = environment + (expression.name to GenericType(emptyList(), newTypeAnnotation))
            val newBody = infer(newEnvironment, expectedType, expression.body)
            ELet(expression.name, newTypeAnnotation, newValue, newBody)
        }

        is EString -> {
            expression.also {
                typeConstraints += CEquality(expectedType, TConstructor("String"))
            }
        }

        is EVariable -> {
            val genericType = environment[expression.name]!!
            val newGenerics = genericType.generics.map {
                freshTypeVariable()
            }
            val instantiation = genericType.generics.zip(newGenerics).toMap<_, Type>()
            val variableType = instantiate(instantiation, genericType.uninstantiatedType)
            if (expression.generics.isNotEmpty()) {
                assert(expression.generics.size == genericType.generics.size)
                expression.generics.zip(newGenerics).forEach { (t, v) ->
                    typeConstraints += CEquality(t, v)
                }
            }
            EVariable(expression.name, newGenerics).also {
                typeConstraints += CEquality(expectedType, variableType)
            }
        }

        is EFunctions -> {
            val recursiveEnvironment = environment + expression.functions.map {
                it.name to (it.typeAnnotation ?: GenericType(emptyList(), freshTypeVariable()))
            }

            val ungeneralizedFunctions = expression.functions.map {
                val uninstantiatedType = recursiveEnvironment[it.name]!!.uninstantiatedType
                it.copy(
                    body = infer(
                        recursiveEnvironment,
                        uninstantiatedType,
                        it.body
                    )
                )
            }

            solveConstraints()

            val newFunctions = ungeneralizedFunctions.map {
                if (it.typeAnnotation != null) {
                    it
                } else {
                    val functionType = recursiveEnvironment[it.name]!!.uninstantiatedType
                    val (newTypeAnnotation, newBody) = generalize(environment, functionType, it.body)
                    it.copy(
                        typeAnnotation = newTypeAnnotation,
                        body = newBody,
                    )
                }
            }

            val newEnvironment = environment + newFunctions.map {
                it.name to it.typeAnnotation!!
            }

            val newBody = infer(newEnvironment, expectedType, expression.body)
            EFunctions(newFunctions, newBody)
        }

        is ESemicolon -> {
            val newBefore = infer(environment, freshTypeVariable(), expression.before)
            val newAfter = infer(environment, expectedType, expression.after)
            ESemicolon(newBefore, newAfter)
        }
    }

public fun instantiate(instantiation: Map<String, Type>, t: Type): Type = when {
    instantiation.isEmpty() -> t
    t is TVariable && substitution[t.index] != t -> {
        instantiate(instantiation, substitution[t.index])
    }

    t is TConstructor -> {
        instantiation[t.name]?.let {
            assert(t.generics.isEmpty())
            it
        } ?: TConstructor(t.name, t.generics.map {
            instantiate(instantiation, it)
        })
    }

    else -> t
}

public fun generalize(environment: Map<String, GenericType>, t: Type, expression: Expr): Pair<GenericType, Expr> {
    val genericTypeVariables = freeInType(t) - freeInEnvironment(environment)
    val genericNames = genericTypeVariables.map {
        it to genericParameterNames.next()
    }
    val localSubstitution = substitution.toMutableList()
    genericNames.forEach { (i, name) ->
        localSubstitution[i] = TConstructor(name)
    }
    val newExpression = substituteExpression(localSubstitution, expression)
    val newType = substitute(localSubstitution, t)
    return GenericType(genericNames.map(Pair<*, String>::second), newType) to newExpression
}

public fun freeInType(t: Type): Set<Int> = when (t) {
    is TVariable -> {
        if (substitution[t.index] != t) {
            freeInType(substitution[t.index])
        } else {
            setOf(t.index)
        }
    }

    is TConstructor -> {
        t.generics.map(::freeInType).fold(emptySet()) { acc, i ->
            acc + i
        }
    }
}

public fun freeInGenericType(t: GenericType): Set<Int> = freeInType(t.uninstantiatedType)

public fun freeInEnvironment(environment: Map<String, GenericType>): Set<Int> =
    environment.values.map(::freeInGenericType).fold(emptySet()) { acc, i ->
        acc + i
    }

public fun substituteExpression(lcoalSubstitution: MutableList<Type>, expression: Expr): Expr = when (expression) {
    is EApply -> {
        val newFunction = substituteExpression(lcoalSubstitution, expression.function)
        val newArguments = expression.arguments.map {
            substituteExpression(lcoalSubstitution, it)
        }
        EApply(newFunction, newArguments)
    }

    is EArray -> {
        val newItemType = expression.itemType?.let {
            substitute(lcoalSubstitution, it)
        }
        val newItems = expression.items.map {
            substituteExpression(lcoalSubstitution, it)
        }
        EArray(newItemType, newItems)
    }

    is EInt -> {
        expression
    }

    is ELambda -> {
        val newReturnType = expression.returnType?.let {
            substitute(lcoalSubstitution, it)
        }
        val newParameters = expression.parameters.map {
            it.copy(typeAnnotation = it.typeAnnotation?.let {
                substitute(lcoalSubstitution, it)
            })
        }
        val newBody = substituteExpression(lcoalSubstitution, expression.body)
        ELambda(newParameters, newReturnType, newBody)
    }

    is ELet -> {
        val newTypeAnnotation = expression.typeAnnotation?.let {
            substitute(lcoalSubstitution, it)
        }
        val newValue = substituteExpression(lcoalSubstitution, expression.value)
        val newBody = substituteExpression(lcoalSubstitution, expression.body)
        ELet(expression.name, newTypeAnnotation, newValue, newBody)
    }

    is EString -> {
        expression
    }

    is EVariable -> {
        val newGenerics = expression.generics.map {
            substitute(lcoalSubstitution, it)
        }
        EVariable(expression.name, newGenerics)
    }

    is EFunctions -> {
        val newFunctions = expression.functions.map {
            val newTypeAnnotation = it.typeAnnotation?.let { genericType ->
                genericType.copy(
                    uninstantiatedType = substitute(lcoalSubstitution, genericType.uninstantiatedType)
                )
            }
            GenericFunction(it.name, newTypeAnnotation, substituteExpression(lcoalSubstitution, it.body))
        }
        val newBody = substituteExpression(lcoalSubstitution, expression.body)

        EFunctions(newFunctions, newBody)
    }

    is ESemicolon -> {
        val newBefore = substituteExpression(lcoalSubstitution, expression.before)
        val newAfter = substituteExpression(lcoalSubstitution, expression.after)
        ESemicolon(newBefore, newAfter)
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

public fun substitute(localSubstitution: MutableList<Type>, t: Type): Type = when (t) {
    is TVariable -> {
        if (localSubstitution[t.index] != t) {
            substitute(localSubstitution, localSubstitution[t.index])
        } else {
            t
        }
    }

    is TConstructor -> {
        TConstructor(t.name, t.generics.map { substitute(localSubstitution, it) })
    }
}

public val initialEnvironment: Map<String, GenericType> = buildMap {
    for (i in listOf("+", "-", "*", "/")) {
        put(
            i,
            GenericType(
                emptyList(),
                TConstructor(
                    "Function2",
                    listOf(
                        TConstructor("Int"),
                        TConstructor("Int"),
                        TConstructor("Int"),
                    )
                )
            )
        )
    }

    for (i in listOf("==", "!=", "<", ">")) {
        put(
            i,
            GenericType(
                emptyList(),
                TConstructor(
                    "Function2",
                    listOf(
                        TConstructor("Int"),
                        TConstructor("Int"),
                        TConstructor("Bool"),
                    )
                )
            )
        )
    }

    put("true", GenericType(emptyList(), TConstructor("Bool")))
    put("false", GenericType(emptyList(), TConstructor("Bool")))

    put(
        "if",
        GenericType(
            listOf("T"),
            TConstructor(
                "Function3",
                listOf(
                    TConstructor("Bool"),
                    TConstructor("Function0", listOf(TConstructor("T"))),
                    TConstructor("Function0", listOf(TConstructor("T"))),
                    TConstructor("T"),
                )
            )
        )
    )
}

public fun infer(expression: Expr): Expr {
    val newExpr = infer(initialEnvironment, freshTypeVariable(), expression)
    solveConstraints()
    return substituteExpression(substitution, newExpr)
}

public fun printInfer(expression: Expr) {
    try {
        println(infer(expression.also(::println)))
    } catch (error: Exception) {
        println(error.message!!)
    }
}

public fun main() {
    printInfer(
        EFunctions(
            listOf(
                GenericFunction(
                    "singleton", null,
                    ELambda(
                        listOf(Parameter("x", null)), null,
                        EArray(null, listOf(EVariable("x", emptyList())))
                    )
                )
            ),
            ESemicolon(
                EApply(EVariable("singleton", emptyList()), listOf(EInt(42))),
                EApply(EVariable("singleton", emptyList()), listOf(EString("foo"))),
            )
        )
    )
    printInfer(
        EFunctions(
            listOf(
                GenericFunction(
                    "compose", null,
                    ELambda(
                        listOf(Parameter("f", null), Parameter("g", null)), null,
                        ELambda(
                            listOf(Parameter("x", null)), null,
                            EApply(
                                EVariable("f"), listOf(
                                    EApply(
                                        EVariable("g"), listOf(
                                            EVariable("x")
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            EVariable("compose")
        )
    )
}