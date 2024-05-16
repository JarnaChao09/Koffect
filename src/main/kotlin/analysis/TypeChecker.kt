package analysis

import analysis.ast.*
import analysis.ast.Type
import lexer.TokenType
import parser.ast.*

// public typealias Environment = Map<String, Set<Type>>

public class TypeChecker(public var environment: Environment) {
    private var currentClass: ClassType? = null
    public fun check(statements: List<Statement>, returnTypes: MutableList<Type> = mutableListOf()): List<TypedStatement> {
        return statements.map {
            when (it) {
                is ClassDeclaration -> {
                    if (this.environment.getClass(it.name.lexeme) != null) {
                        error("Class ${it.name.lexeme} is already defined")
                    }

                    val currentClassType = ClassType(
                        it.name.lexeme,
                        null, // todo: superclasses
                        emptyList(), // todo: interfaces
                        mutableMapOf(),
                        mutableMapOf(),
                    )
                    val previousCurrentClass = this.currentClass
                    this.currentClass = currentClassType

                    this.environment.addClass(it.name.lexeme, currentClassType)
                    this.environment.addVariable(
                        it.name.lexeme,
                        FunctionType(
                            it.name.lexeme,
                            mutableSetOf(
                                FunctionType.Overload(
                                emptyList(),
                                VariableType(it.name.lexeme)
                            ))
                        )
                    )
                    this.environment = Environment(this.environment)

                    val typedFields = check(it.field)
                    val typedMethods = check(it.methods)

                    this.environment = this.environment.enclosing!!

                    // todo: superclasses
                    val superClassType = it.superClass?.let { superClass ->
                        superClass as TConstructor
                        VariableType(superClass.name)
                    }

                    // todo: interfaces
                    val interfaceTypes = it.interfaces.map { i ->
                        i as TConstructor
                        VariableType(i.name)
                    }

                    this.currentClass = previousCurrentClass

                    @Suppress("UNCHECKED_CAST")
                    TypedClassDeclaration(
                        it.name,
                        superClassType,
                        interfaceTypes,
                        typedFields as List<TypedVariableStatement>,
                        typedMethods as List<TypedFunctionDeclaration>,
                    )
                }
                is ExpressionStatement -> {
                    TypedExpressionStatement(it.expression.toTypedExpression())
                }
                is IfStatement -> {
                    val typedCondition = it.condition.toTypedExpression()

                    require(typedCondition.type == VariableType("Boolean")) {
                        error("Condition expected to return a Boolean, but a ${typedCondition.type} was found")
                    }

                    val typedTrueBranch = check(it.trueBranch, returnTypes)
                    val typedFalseBranch = check(it.falseBranch, returnTypes)

                    TypedIfStatement(it.condition.toTypedExpression(), typedTrueBranch, typedFalseBranch)
                }
                is FunctionDeclaration -> {
                    val name = it.name.lexeme
                    val typedParameters = it.parameters.map { (name, type) ->
                        TypedFunctionDeclaration.Parameter(name, VariableType(type.lexeme))
                    }
                    val returnType = VariableType(it.returnType.lexeme)

                    var oldFunctionType = this.environment.getVariable(name)

                    if (oldFunctionType == null) {
                        val funcType = FunctionType(name)
                        oldFunctionType = funcType
                        this.environment.addVariable(name, oldFunctionType)
                    } else {
                        require(oldFunctionType is FunctionType) {
                            "Function overloads cannot shadow variables currently" // todo: update environment to allow for both variables and functions to have the same identifier
                        }
                    }

                    val parameterTypes = typedParameters.map { (_, type) -> type }

                    oldFunctionType.addOverload(parameterTypes, returnType)
                    this.currentClass?.run {
                        addFunction(it.name.lexeme, parameterTypes, returnType)
                    }

                    this.environment = Environment(this.environment)

                    typedParameters.forEach { (parameterName, parameterType) ->
                        this.environment.addVariable(parameterName.lexeme, parameterType)
                    }

                    val returns = mutableListOf<Type>()

                    val typedBody = check(it.body, returns)

                    if (returns.isEmpty() && returnType != VariableType("Unit")) {
                        error("Expected function $name to return $returnType but found Unit")
                    }

                    for (type in returns) {
                        if (type != returnType) {
                            error("Expected function $name to return $returnType but found $type instead")
                        }
                    }

                    this.environment = this.environment.enclosing!!

                    TypedFunctionDeclaration(it.name, typedParameters, returnType, typedBody)
                }
                is VariableStatement -> {
                    val typeToken = it.type ?: error("Variables must be annotated with a type (type inference is not implemented)")
                    val type = VariableType(typeToken.lexeme)

                    val typedInitializer = it.initializer?.toTypedExpression()
                    val initializerType = typedInitializer?.type

                    initializerType?.let { initType ->
                        if (initType != type) {
                            error("Variable initializer does not match declared type, found $initType but expected $type")
                        }
                    }

                    this.environment.addVariable(it.name.lexeme, type)

                    this.currentClass?.run {
                        addProperty(it.name.lexeme, type)
                    }

                    TypedVariableStatement(it, type, typedInitializer)
                }
                is WhileStatement -> {
                    val typedCondition = it.condition.toTypedExpression()

                    require(typedCondition.type == VariableType("Boolean")) {
                        error("Condition expected to return a Boolean, but a ${typedCondition.type} was found")
                    }

                    val typedBody = check(it.body, returnTypes)

                    TypedWhileStatement(typedCondition, typedBody)
                }
                is ReturnStatement -> {
                    val typedReturnExpression = it.value?.toTypedExpression()

                    val returnType = typedReturnExpression?.type ?: VariableType("Unit")

                    returnTypes.add(returnType)

                    TypedReturnStatement(it.keyword, typedReturnExpression)
                }
            }
        }
    }

    private fun Expression.toTypedExpression(): TypedExpression {
        return when (this) {
            is Assign -> {
                val typedAssignment = this.expression.toTypedExpression()
                val variableType = this@TypeChecker.environment.getVariable(this.name.lexeme) ?: error("Undefined variable ${this.name.lexeme}")

                if (typedAssignment.type == variableType) {
                    TypedAssign(this.name, typedAssignment)
                } else {
                    error("Unable to assign value of type ${typedAssignment.type} to variable ${this.name.lexeme} with type $variableType")
                }
            }
            is Binary -> {
                val leftTypedExpression = this.left.toTypedExpression()
                val rightTypedExpression = this.right.toTypedExpression()

                val function = when (this.operator.type) {
                    TokenType.PLUS -> "plus"
                    TokenType.MINUS -> "minus"
                    TokenType.STAR -> "times"
                    TokenType.SLASH -> "div"
                    TokenType.MOD -> "mod"
                    TokenType.EQUALS,
                    TokenType.NOT_EQ,
                    TokenType.GE,
                    TokenType.LE,
                    TokenType.GT,
                    TokenType.LT -> this.operator.lexeme // reminder todo: update to compareTo
                    else -> error("Custom binary operators are unsupported. Invalid Binary Operator ${this.operator.lexeme}") // should be unreachable for now
                }

                val leftTypeName = when (val leftType = leftTypedExpression.type) {
                    is VariableType -> leftType.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val receiverReference = this@TypeChecker.environment.getClass(leftTypeName) ?: error("Unknown class '$leftTypeName'")

                val functionReference = receiverReference.functions[function] ?: error("Unknown function '$function' with receiver type '$leftTypeName'")

                var returnType: Type? = null

                for (functionOverload in functionReference.functionType.overloads) {
                    // todo: update to check for operator status once operator distinction is added
                    if (functionOverload.arity != 1) {
                        continue
                    }

                    if (rightType == functionOverload.parameterTypes[0]) {
                        returnType = functionOverload.returnType
                        break
                    }
                }

                if (returnType == null) {
                    error("Unable to find function definition on type $leftTypeName for $function with parameter $rightTypeName. Known candidates are: ${functionReference.functionType}")
                }

                TypedBinary(leftTypedExpression, this.operator, rightTypedExpression, returnType)
            }
            is Call -> {
                val typedCallee = this.callee.toTypedExpression()
                val calleeType = typedCallee.type

                require(calleeType is FunctionType) {
                    "Invoke on custom types is currently unsupported. Callee must be a function."
                }

                val typedArguments = this.arguments.map {
                    it.toTypedExpression()
                }

                // todo: return back to figure out a solution to give better error diagnostics
                var found: Type? = null
                for (functionOverload in calleeType.overloads) {
                    if (functionOverload.arity != typedArguments.size) {
                        continue // error diagnostic?
                    }

                    var acc = true
                    for (i in typedArguments.indices) {
                        val argumentType = typedArguments[i].type

                        acc = acc && argumentType == functionOverload.parameterTypes[i]
                    }

                    if (acc) {
                        found = functionOverload.returnType
                        break // return type based overload resolution?
                    }
                }

                if (found == null) {
                    error("No valid function matching the call signature for ${calleeType.name} was found. Known candidates are: $calleeType")
                }

                TypedCall(typedCallee, this.paren, typedArguments, found)
            }
            is Get -> {
                val typedInstance = this.instance.toTypedExpression()

                val receiverName = when (val type = typedInstance.type) {
                    is VariableType -> type.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val classRef = this@TypeChecker.environment.getClass(receiverName) ?: error("Unknown class '$receiverName'")

                // todo: new ast node for getting a function?
                val getType = classRef.properties[this.name.lexeme]?.type ?: classRef.functions[this.name.lexeme]?.functionType ?: error("Unknown property ${this.name.lexeme} on class '$receiverName'")

                TypedGet(typedInstance, this.name, getType)
            }
            is Grouping -> {
                TypedGrouping(this.expression.toTypedExpression())
            }
            is IfExpression -> {
                val typedCondition = this.condition.toTypedExpression()

                val typedTrueBranch = check(this.trueBranch)
                val trueType = when (val trueBranchLast = typedTrueBranch.lastOrNull()) {
                    is TypedExpressionStatement -> trueBranchLast.expression.type
                    else -> VariableType("Unit")
                }

                val typedFalseBranch = check(this.falseBranch)
                val falseType = when (val falseBranchLast = typedFalseBranch.lastOrNull()) {
                    is TypedExpressionStatement -> falseBranchLast.expression.type
                    else -> VariableType("Unit")
                }

                require(typedCondition.type == VariableType("Boolean")) {
                    "the conditional expression must return a type of Boolean, found ${typedCondition.type}"
                }

                require(trueType == falseType) {
                    "the types of the if branches must be the same, $trueType != $falseType"
                }

                TypedIfExpression(typedCondition, typedTrueBranch, typedFalseBranch, trueType)
            }
            is BooleanLiteral, is DoubleLiteral, is IntLiteral, NullLiteral, is StringLiteral -> TypedLiteral(this as Literal<*>)
            is Logical -> {
                val leftTypedExpression = this.left.toTypedExpression()
                val rightTypedExpression = this.right.toTypedExpression()
                val function = this.operator.lexeme

                val leftTypeName = when (val leftType = leftTypedExpression.type) {
                    is VariableType -> leftType.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val receiverReference = this@TypeChecker.environment.getClass(leftTypeName) ?: error("Unknown class '$leftTypeName'")

                val functionReference = receiverReference.functions[function] ?: error("Unknown function '$function' with receiver type '$leftTypeName'")

                var returnType: Type? = null

                for (functionOverload in functionReference.functionType.overloads) {
                    if (functionOverload.arity != 1) {
                        continue
                    }

                    if (rightType == functionOverload.parameterTypes[0]) {
                        returnType = functionOverload.returnType
                        break
                    }
                }

                if (returnType == null) {
                    error("Unable to find function definition on type $leftTypeName for $function with parameter $rightTypeName. Known candidates are: ${functionReference.functionType}")
                }

                if (returnType != VariableType("Boolean")) {
                    error("Logical operations must return type Boolean") // should be unreachable
                }

                TypedLogical(leftTypedExpression, this.operator, rightTypedExpression)
            }
            is This -> {
                TypedThis(
                    this.keyword,
                    VariableType(
                        this@TypeChecker.currentClass?.name ?: error("Invalid use of 'this' when not inside a class scope")
                    )
                )
            }
            is Unary -> {
                val typedExpression = this.expression.toTypedExpression()
                val function = when (this.operator.type) {
                    TokenType.PLUS -> "unaryPlus"
                    TokenType.MINUS -> "unaryMinus"
                    TokenType.NOT -> "not"
                    else -> error("Custom Unary Operators are unsupported. Invalid Unary Operator ${this.operator.lexeme}")
                }
                val receiverTypeName = when (val receiverType = typedExpression.type) {
                    is VariableType -> receiverType.name
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val receiverReference = this@TypeChecker.environment.getClass(receiverTypeName) ?: error("Unknown class '$receiverTypeName'")

                val functionReference = receiverReference.functions[function] ?: error("Unknown function '$function' with receiver type '$receiverTypeName'")

                var returnType: Type? = null

                for (functionOverload in functionReference.functionType.overloads) {
                    // todo: update to check for operator status once operator distinction is added
                    if (functionOverload.arity != 0) {
                        continue
                    }

                    returnType = functionOverload.returnType
                }

                if (returnType == null) {
                    error("Unable to find function definition on type $receiverTypeName for $function. Known candidates are: ${functionReference.functionType}")
                }

                TypedUnary(this.operator, typedExpression, returnType)
            }
            is Variable -> {
                val variableType = this@TypeChecker.environment.getVariable(this.name.lexeme) ?: error("Undefined variable ${this.name.lexeme}")
                TypedVariable(this.name, variableType)
            }
        }
    }
}