package analysis

import lexer.TokenType
import parser.ast.*
import parser.ast.Grouping

public typealias Environment = Map<String, Set<Type>>

public class TypeChecker(public var environment: Environment) {
    public fun check(statements: List<Statement>) {
        statements.forEach {
            when (it) {
                is ExpressionStatement -> it.expression.check()
                is IfStatement -> {
                    when (val conditionType = it.condition.check()) {
                        is TConstructor -> if (conditionType.name != "Boolean") {
                            error("Condition expected to return a Boolean, but a ${conditionType.name} was found")
                        }
                    }
                    check(it.trueBranch)
                    check(it.falseBranch)
                }
                is FunctionDeclaration -> {
                    val name = it.name.lexeme
                    val parameterTypes = it.parameters.map { (_, type) -> type }
                    val returnType = it.returnType

                    val functionType = TConstructor("Function${it.arity}", parameterTypes + listOf(returnType))

                    val oldFunctionType = this.environment.getOrDefault(name, setOf())

                    this.environment += (name to setOf(functionType) + oldFunctionType)
                }
                is VariableStatement -> {
                    val type = it.type ?: error("Variables must be annotated with a type (type inference is not implemented)")
                    val initializerType = it.initializer?.check()

                    initializerType?.let { initType ->
                        if (initType != type) {
                            error("Variable initializer does not match declared type, found $initType but expected $type")
                        }
                    }

                    this.environment += (it.name.lexeme to setOf(type))
                }
                is WhileStatement -> {
                    when (val conditionType = it.condition.check()) {
                        is TConstructor -> if (conditionType.name != "Boolean") {
                            error("Condition expected to return a Boolean, but a ${conditionType.name} was found")
                        }
                    }
                    check(it.body)
                }
            }
        }
    }

    private fun Expression.check(): Type {
        return when (this) {
            is Assign -> {
                val assignment = this.expression.check()
                val type = this@TypeChecker.environment[this.name.lexeme]!!.first()

                if (type == assignment) {
                    this.type = type
                    type
                } else {
                    error("Unable to assign type $assignment to type $type")
                }
            }
            is Binary -> {
                val leftType = this.left.check()
                val rightType = this.right.check()
                val function = when (this.operator.type) {
                    TokenType.PLUS -> "plus"
                    TokenType.MINUS -> "minus"
                    TokenType.STAR -> "times"
                    TokenType.SLASH -> "div"
                    TokenType.MOD -> "mod"
                    TokenType.EQUALS, TokenType.NOT_EQ, TokenType.GE, TokenType.LE, TokenType.GT, TokenType.LT ->
                        this.operator.lexeme
                    else -> error("Invalid Binary Operator") // should be unreachable
                }

                var found: Type? = null

                for (currentType in this@TypeChecker.environment[function]!!) {
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
            is Call -> {
                val calleeType = when (this.callee) {
                    is Variable -> this@TypeChecker.environment[this.callee.name.lexeme]!!
                    else -> error("Invalid Callee, expected a variable")
                }
                val paramTypes = this.arguments.map {
                    it.check()
                }

                // todo: return back to figure out a solution to give better error diagnostics
                var found: Type? = null
                for (possibleType in calleeType) {
                    when (possibleType) {
                        is TConstructor -> {
                            if ("Function" !in possibleType.name) {
                                error("Invalid call target: ${this.callee} is not of type Function")
                            } else if (paramTypes.size != possibleType.generics.size - 1) {
                                // error("Invalid number of arguments for call to ${this.callee}: got ${paramTypes.size} but expected ${possibleType.generics.size - 1}")
                            } else {
                                var acc = true
                                for (i in paramTypes.indices) {
                                    when (val paramType = paramTypes[i]) {
                                        is TConstructor -> {
                                            when (val argType = possibleType.generics[i]) {
                                                is TConstructor -> {
                                                    acc = acc && paramType == argType
                                                }
                                            }
                                        }
                                    }
                                }

                                if (acc) {
                                    found = possibleType
                                    break
                                }
                            }
                        }
                    }
                }

                when (found) {
                    is TConstructor -> {
                        found.generics.last().also {
                            this.callee.type = found
                            this.type = it
                        }
                    }
                    null-> {
                        error("No valid function matching the call signature for ${this.callee.name} was found")
                    }
                }
            }
            is Grouping -> {
                val type = this.expression.check()
                this.type = type
                type
            }
            is IfExpression -> {
                val conditionType = this.condition.check()
                check(this.trueBranch)
                check(this.falseBranch)
                val trueType: Type = when (val trueBranchLast = this.trueBranch.last()) {
                    is ExpressionStatement -> trueBranchLast.expression.type!!
                    else -> TConstructor("Unit")
                }
                val falseType = when (val falseBranchLast = this.falseBranch.last()) {
                    is ExpressionStatement -> falseBranchLast.expression.type!!
                    else -> TConstructor("Unit")
                }

                require(conditionType == TConstructor("Boolean")) {
                    "the conditional must be of type Boolean, found $conditionType"
                }

                require(trueType == falseType) {
                    "the types of the branches must be the same, $trueType != $falseType"
                }

                this.type = trueType

                trueType
            }
            is DoubleLiteral, is IntLiteral, is BooleanLiteral, NullLiteral, is ObjectLiteral<*> -> this.type!!
            is Logical -> {
                val leftType = this.left.check()
                val rightType = this.right.check()
                val function = this.operator.lexeme

                var found: Type? = null

                for (currentType in this@TypeChecker.environment[function]!!) {
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
                    TokenType.NOT -> "not"
                    else -> error("Invalid Unary Operator") // should be unreachable
                }

                var found: Type? = null

                for (currentType in this@TypeChecker.environment[function]!!) {
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
            is Variable -> {
                val type = environment[this.name.lexeme]!!

                if (type.size != 1) {
                    error("Variable has more than one possible type, ambiguous variable")
                }

                type.first().also {
                    this.type = it
                }
            }
        }
    }
}