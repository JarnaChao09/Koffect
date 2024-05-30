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
    val primaryConstructor: TypedConstructor?,
    val secondaryConstructors: List<TypedConstructor>,
    val superClass: Type?,
    val interfaces: List<Type>,
    val fields: List<TypedVariableStatement>,
    val methods: List<TypedFunctionDeclaration>,
) : TypedDeclaration {
    public data class TypedConstructor(val parameters: List<TypedConstructorParameter>)

    public data class TypedConstructorParameter(
        val name: Token,
        val type: Type,
        val fieldType: FieldType,
        val value: TypedExpression?,
    )

    public enum class FieldType {
        VAL,
        VAR,
        NONE,
    }

    private fun printPrimaryConstructor(): String {
        return this.primaryConstructor?.let {
            "(${it.parameters.joinToString(", ") { param ->
                "${when (param.fieldType) {
                    FieldType.VAL -> "val "
                    FieldType.VAR -> "var "
                    FieldType.NONE -> ""
                }}${param.name.lexeme}${param.value?.let { v ->
                    " = $v"
                } ?: ""}"
            }})"
        } ?: ""
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
                |class ${this.name.lexeme}${this.printPrimaryConstructor()}${this.printInheritors()} {
                |${this.fields.joinToString("\n")}
                |${this.methods.joinToString("\n")}
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

public data class TypedFunctionDeclaration(
    val name: Token,
    val parameters: List<TypedParameter>,
    val returnType: Type,
    val body: List<TypedStatement>,
) : TypedDeclaration {
    public data class TypedParameter(val name: Token, val type: Type) {
        override fun toString(): String {
            return "${name.lexeme}: $type"
        }
    }

    public val arity: Int
        get() = this.parameters.size

    override fun toString(): String {
        return "fun ${this.name.lexeme}(${this.parameters.joinToString(", ")}): ${this.returnType} {\n${this.body.joinToString("\n")}\n}"
    }
}

public data class TypedIfStatement(
    val condition: TypedExpression,
    val trueBranch: List<TypedStatement>,
    val falseBranch: List<TypedStatement>,
) : TypedStatement {
    override fun toString(): String {
        return "if (${this.condition}) {\n${this.trueBranch.joinToString("\n")}\n} else {\n${this.falseBranch.joinToString("\n")}\n}"
    }
}

public sealed interface TypedVariableStatement : TypedStatement {
    public val name: Token

    public val type: Type?

    public val initializer: TypedExpression?

    public companion object {
        public operator fun invoke(variableStatement: VariableStatement, variableType: Type, typedInitializer: TypedExpression?): TypedVariableStatement {
            return when (variableStatement) {
                is Val -> TypedVal(variableStatement.name, variableType, typedInitializer)
                is Var -> TypedVar(variableStatement.name, variableType, typedInitializer)
            }
        }
    }
}

public data class TypedVar(override val name: Token, override val type: Type?, override val initializer: TypedExpression?) : TypedVariableStatement {
    override fun toString(): String {
        return "var ${this.name.lexeme}: ${this.type} = ${this.initializer ?: "uninitialized"}"
    }
}

public data class TypedVal(override val name: Token, override val type: Type?, override val initializer: TypedExpression?) : TypedVariableStatement {
    override fun toString(): String {
        return "val ${this.name.lexeme}: ${this.type} = ${this.initializer ?: "uninitialized"}"
    }
}

public data class TypedReturnStatement(val keyword: Token, val value: TypedExpression?) : TypedStatement {
    override fun toString(): String {
        return "return ${this.value ?: "Unit"}"
    }
}

public data class TypedWhileStatement(val condition: TypedExpression, val body: List<TypedStatement>) : TypedStatement {
    override fun toString(): String {
        return "while (${this.condition}) {\n${this.body.joinToString("\n")}\n}"
    }
}