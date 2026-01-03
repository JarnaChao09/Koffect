package parser.ast

import lexer.Token
import lexer.TokenType

public sealed interface Statement

public sealed interface Declaration : Statement

public data class ClassDeclaration(
    val name: Token,
    val primaryConstructor: PrimaryConstructor?,
    val secondaryConstructors: List<SecondaryConstructor>,
    val superClass: Token?,
    val interfaces: List<Token>,
    val fields: List<VariableStatement>,
    val methods: List<FunctionDeclaration>,
) : Declaration {
    public data class PrimaryConstructor(val parameters: List<Parameter>, val parameterType: List<FieldType>)

    public data class SecondaryConstructor(val parameters: List<Parameter>, val delegatedArguments: List<Expression>, val body: List<Statement>)

    public enum class FieldType {
        VAL,
        VAR,
        NONE,
    }
}

public data class ExpressionStatement(public val expression: Expression) : Statement

public data class FunctionDeclaration(
    val name: Token,
    val receiver: Type?,
    val contexts: List<Type>,
    val parameters: List<Parameter>,
    val returnType: Type,
    val body: List<Statement>,
    val inline: Boolean,
) : Declaration {
    public val arity: Int
        get() = this.parameters.size
}

public data class Parameter(val name: Token, val type: Type, val value: Expression?)

public data class IfStatement(
    val condition: Expression,
    val trueBranch: List<Statement>,
    val falseBranch: List<Statement>,
) : Statement

public sealed interface VariableStatement : Statement {
    public val name: Token

    public val type: Type?

    public val initializer: Expression?

    public companion object {
        public operator fun invoke(variableType: TokenType, name: Token, type: Type?, init: Expression?): VariableStatement {
            return when (variableType) {
                TokenType.VAL -> Val(name, type, init)
                TokenType.VAR -> Var(name, type, init)
                else -> error("unreachable: should only be triggered with val/var")
            }
        }
    }
}

public data class Var(override val name: Token, override val type: Type?, override val initializer: Expression?) : VariableStatement

public data class Val(override val name: Token, override val type: Type?, override val initializer: Expression?) : VariableStatement

public data class DeleteStatement(val keyword: Token, val reason: Expression?) : Statement

public data class ReturnStatement(val keyword: Token, val value: Expression?) : Statement

public data class WhileStatement(val condition: Expression, val body: List<Statement>) : Statement