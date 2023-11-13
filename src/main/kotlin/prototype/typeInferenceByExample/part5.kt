package prototype.typeInferenceByExample

public sealed interface Type

public data class TConstructor(val name: String, val generics: List<Type> = listOf()) : Type

public data class TVariable(val index: Int) : Type

public sealed interface Expr

public data class ELambda(val x: String, val e: Expr) : Expr

public data class EApply(val e1: Expr, val e2: Expr) : Expr

public data class EVariable(val x: String) : Expr

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
                        require(occursIn(t1.index, t2))
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

public fun inferType(expression: Expr, environment: Map<String, Type>): Type = when (expression) {
    is ELambda -> {
        val x = expression.x
        val e = expression.e
        val t1 = freshTypeVariable()
        val environment2 = environment + (x to t1)
        val t2 = inferType(e, environment2)
        TConstructor("Function1", listOf(t1, t2))
    }

    is EApply -> {
        val e1 = expression.e1
        val e2 = expression.e2

        val t1 = inferType(e1, environment)
        val t2 = inferType(e2, environment)
        val t3 = freshTypeVariable()
        typeConstraints += CEquality(t1, TConstructor("Function1", listOf(t2, t3)))
        t3
    }

    is EVariable -> {
        environment[expression.x]!!
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
                "Function1",
                listOf(TConstructor("Int"), TConstructor("Function1", listOf(TConstructor("Int"), TConstructor("Int"))))
            )
        )
    }

    for (i in 0..99) {
        put(i.toString(), TConstructor("Int"))
    }
}

public fun infer(expression: Expr): Type {
    val t = inferType(expression, initialEnvironment)
    solveConstraints()
    return substitute(t)
}

public fun main() {
    println(
        infer(
            ELambda("x", EApply(EApply(EVariable("+"), EVariable("x")), EVariable("x")))
        )
    )
}