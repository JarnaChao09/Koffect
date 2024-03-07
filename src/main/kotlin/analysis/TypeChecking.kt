package analysis

import lexer.TokenType
import parser.ast.*
import parser.ast.Grouping

public typealias Environment = Map<String, Set<Type>>

public class TypeChecking(private val environment: Environment) {
    public fun check(statements: List<Statement>) {
        statements.forEach {
            when (it) {
                is ExpressionStatement -> it.expression.check()
            }
        }
    }

    private fun Expression.check(): Type {
        return when (this) {
            is Binary -> {
                val leftType = this.left.check()
                val rightType = this.right.check()
                val function = when (this.operator.type) {
                    TokenType.PLUS -> "plus"
                    TokenType.MINUS -> "minus"
                    TokenType.STAR -> "times"
                    TokenType.SLASH -> "div"
                    TokenType.EQUALS, TokenType.NOT_EQ, TokenType.GE, TokenType.LE, TokenType.GT, TokenType.LT ->
                        this.operator.lexeme
                    else -> error("Invalid Binary Operator") // should be unreachable
                }

                var found: Type? = null

                for (currentType in this@TypeChecking.environment[function]!!) {
                    when (currentType) {
                        is TConstructor -> {
                            if (leftType == currentType.generics[0] && rightType == currentType.generics[1]) {
                                found = currentType.generics[2]
                                break
                            }
                        }
                    }
                }

                this.type = found

                found ?: error("Invalid Binary Operator, could not find definition using types $leftType and $rightType")
            }
            is Grouping -> {
                val type = this.expression.check()
                this.type = type
                type
            }
            is DoubleLiteral, is IntLiteral, is BooleanLiteral, NullLiteral -> this.type!!
            is Logical -> {
                val leftType = this.left.check()
                val rightType = this.right.check()
                val function = this.operator.lexeme

                var found: Type? = null

                for (currentType in this@TypeChecking.environment[function]!!) {
                    when (currentType) {
                        is TConstructor -> {
                            if (leftType == currentType.generics[0] && rightType == currentType.generics[1]) {
                                found = this.type!!.takeIf { it == currentType.generics[2] }
                                break
                            }
                        }
                    }
                }

                found ?: error("Invalid Logical Operator, could not find definition using types $leftType and $rightType that returned Boolean")
            }
            is Unary -> {
                val expressionType = this.expression.check()
                val function = when (this.operator.type) {
                    TokenType.PLUS -> "unaryPlus"
                    TokenType.MINUS -> "unaryMinus"
                    else -> error("Invalid Unary Operator") // should be unreachable
                }

                var found: Type? = null

                for (currentType in this@TypeChecking.environment[function]!!) {
                    when (currentType) {
                        is TConstructor -> {
                            if (expressionType == currentType.generics[0]) {
                                found = currentType.generics[1]
                                break
                            }
                        }
                    }
                }

                this.type = found

                found ?: error("Invalid Unary Operator, could not find definition using type $expressionType")
            }
        }
    }
}