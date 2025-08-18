package analysis.ast

import lexer.Token
import parser.ast.Val
import parser.ast.Var
import parser.ast.VariableStatement

public sealed interface TypedStatement {
    // public val type: Type
}

public sealed interface TypedDeclaration : TypedStatement

public data class TypedClassDeclaration(
    val name: Token,
    val primaryConstructor: TypedPrimaryConstructor?,
    val secondaryConstructors: List<TypedSecondaryConstructor>,
    val superClass: Type?,
    val interfaces: List<Type>,
    val fields: List<TypedVariableStatement>,
    val methods: List<TypedFunctionDeclaration>,
) : TypedDeclaration {
    public data class TypedPrimaryConstructor(
        val parameters: List<TypedParameter>,
        val parameterTypes: List<FieldType>,
    ) {
        override fun toString(): String {
            return buildString {
                append("(")
                this@TypedPrimaryConstructor.parameters.zip(this@TypedPrimaryConstructor.parameterTypes)
                    .forEach { (param, paramType) ->
                        append(
                            when (paramType) {
                                FieldType.VAL -> "val "
                                FieldType.VAR -> "var "
                                FieldType.NONE -> ""
                            }
                        )
                        append(param)
                    }
                append(")")
            }
        }
    }

    public data class TypedSecondaryConstructor(val parameters: List<TypedParameter>, val delegatedArguments: List<TypedExpression>, val body: List<TypedStatement>) {
        override fun toString(): String {
            return "constructor(${this.parameters.joinToString(", ")}) : this(${this.delegatedArguments.joinToString(", ")}) {\n${this.body.joinToString("\n").prependIndent()}\n}"
        }
    }

    public enum class FieldType {
        VAL,
        VAR,
        NONE,
    }

    private fun printInheritors(): String {
        return if (this.superClass == null && this.interfaces.isEmpty()) {
            ""
        } else {
            " : ${this.superClass?.let { "$it, " } ?: ""}${this.interfaces.joinToString(", ")}"
        }
    }

    override fun toString(): String {
        val ret =
            """
                |class ${this.name.lexeme}${this.primaryConstructor ?: "()"}${this.printInheritors()} {
                |${this.secondaryConstructors.joinToString("\n").prependIndent()}
                |${this.fields.joinToString("\n").prependIndent()}
                |${this.methods.joinToString("\n").prependIndent()}
                |}
            """.trimMargin()

        return ret
    }
}

public data class TypedExpressionStatement(public val expression: TypedExpression) : TypedStatement {
    override fun toString(): String {
        return this.expression.toString()
    }
}

public data class TypedReturnExpressionStatement(public val returnExpression: TypedExpression) : TypedStatement {
    override fun toString(): String {
        return "${this.returnExpression}^"
    }
}

public data class TypedFunctionDeclaration(
    val name: Token,
    val mangledName: String,
    val contexts: List<Type>,
    val parameters: List<TypedParameter>,
    val returnType: Type,
    val body: List<TypedStatement>,
    val deleted: Boolean,
) : TypedDeclaration {
    public val arity: Int
        get() = this.parameters.size

    override fun toString(): String {
        return "${if (this.contexts.isNotEmpty()) "context(${this.contexts.joinToString(", ")}) " else ""}fun ${this.name.lexeme}(${this.parameters.joinToString(", ")}): ${this.returnType} {\n${
            this.body.joinToString(
                "\n"
            ).prependIndent()
        }\n}"
    }
}

public data class TypedParameter(val name: Token, val type: Type, val value: TypedExpression?) {
    override fun toString(): String {
        return "${this.name.lexeme}: ${this.type}${
            this.value?.let { v ->
                " = $v"
            } ?: ""
        }"
    }
}

public data class TypedIfStatement(
    val condition: TypedExpression,
    val trueBranch: List<TypedStatement>,
    val falseBranch: List<TypedStatement>,
) : TypedStatement {
    override fun toString(): String {
        return "if (${this.condition}) {\n${this.trueBranch.joinToString("\n").prependIndent()}\n} else {\n${
            this.falseBranch.joinToString(
                "\n"
            ).prependIndent()
        }\n}"
    }
}

public sealed interface TypedVariableStatement : TypedStatement {
    public val name: Token

    public val type: Type?

    public val initializer: TypedExpression?

    public companion object {
        public operator fun invoke(
            variableStatement: VariableStatement,
            variableType: Type,
            typedInitializer: TypedExpression?,
        ): TypedVariableStatement {
            return when (variableStatement) {
                is Val -> TypedVal(variableStatement.name, variableType, typedInitializer)
                is Var -> TypedVar(variableStatement.name, variableType, typedInitializer)
            }
        }
    }
}

public data class TypedVar(
    override val name: Token,
    override val type: Type?,
    override val initializer: TypedExpression?,
) : TypedVariableStatement {
    override fun toString(): String {
        return "var ${this.name.lexeme}: ${this.type} = ${this.initializer ?: "uninitialized"}"
    }
}

public data class TypedVal(
    override val name: Token,
    override val type: Type?,
    override val initializer: TypedExpression?,
) : TypedVariableStatement {
    override fun toString(): String {
        return "val ${this.name.lexeme}: ${this.type} = ${this.initializer ?: "uninitialized"}"
    }
}

public data class TypedDeleteStatement(val keyword: Token, val reason: TypedExpression?) : TypedStatement {
    override fun toString(): String {
        return "delete${reason?.let { "($it)" } ?: ""}"
    }
}

public data class TypedReturnStatement(val keyword: Token, val value: TypedExpression?) : TypedStatement {
    override fun toString(): String {
        return "return ${this.value ?: "Unit"}"
    }
}

public data class TypedWhileStatement(val condition: TypedExpression, val body: List<TypedStatement>) : TypedStatement {
    override fun toString(): String {
        return "while (${this.condition}) {\n${this.body.joinToString("\n").prependIndent()}\n}"
    }
}