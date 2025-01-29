package analysis

import analysis.ast.*
import analysis.ast.Type
import lexer.TokenType
import parser.ast.*

// public typealias Environment = Map<String, Set<Type>>

public class TypeChecker(public var environment: Environment) {
    private var currentClass: ClassType? = null

    private enum class Scope {
        TOP_LEVEL,
        FUNCTION_LEVEL,
        CLASS_LEVEL,
    }

    private var scope: Scope = Scope.TOP_LEVEL

    public fun check(statements: List<Statement>, returnTypes: MutableList<Type> = mutableListOf()): List<TypedStatement> {
        fun Parameter.toTypedParameter(): TypedParameter {
            val parameterType = this.type.toType()

            val typedValue = this.value?.let { v ->
                v.toTypedExpression().also { tv ->
                    require(tv.type == parameterType) {
                        "Type Mismatch: Parameter ${this.name.lexeme} expected type $parameterType but found ${tv.type}"
                    }
                }
            }

            return TypedParameter(
                this.name,
                parameterType,
                typedValue,
            )
        }

        return statements.map {
            when (it) {
                is ClassDeclaration -> {
                    fun ClassDeclaration.PrimaryConstructor.toTypedConstructor(): TypedClassDeclaration.TypedPrimaryConstructor {
                        return TypedClassDeclaration.TypedPrimaryConstructor(
                            this.parameters.map(Parameter::toTypedParameter),
                            this.parameterType.map { fieldType ->
                                when (fieldType) {
                                    ClassDeclaration.FieldType.VAL -> TypedClassDeclaration.FieldType.VAL
                                    ClassDeclaration.FieldType.VAR -> TypedClassDeclaration.FieldType.VAR
                                    ClassDeclaration.FieldType.NONE -> TypedClassDeclaration.FieldType.NONE
                                }
                            }
                        )
                    }
                    fun ClassDeclaration.SecondaryConstructor.toTypedConstructor(): TypedClassDeclaration.TypedSecondaryConstructor {
                        this@TypeChecker.environment = Environment(this@TypeChecker.environment)

                        val typedParameters = this.parameters.map { param ->
                            val tp = param.toTypedParameter()

                            this@TypeChecker.environment.addVariable(tp.name.lexeme, tp.type)

                            tp
                        }
                        val typedDelegatedArguments = this.delegatedArguments.map { arg ->
                            arg.toTypedExpression()
                        }
                        val typedBody = check(this.body)

                        this@TypeChecker.environment = this@TypeChecker.environment.enclosing!!

                        return TypedClassDeclaration.TypedSecondaryConstructor(typedParameters, typedDelegatedArguments, typedBody)
                    }

                    if (this.environment.getClass(it.name.lexeme) != null) {
                        error("Class ${it.name.lexeme} is already defined")
                    }

                    val previousScope = this.scope
                    this.scope = Scope.CLASS_LEVEL

                    // todo: superclasses
                    val superClassType = it.superClass?.let { superClass ->
                        VariableType(superClass.lexeme)
                    }

                    // todo: interfaces
                    val interfaceTypes = it.interfaces.map { i ->
                        VariableType(i.lexeme)
                    }

                    val primaryConstructor = it.primaryConstructor?.toTypedConstructor()

                    val secondaryConstructors = it.secondaryConstructors.map(ClassDeclaration.SecondaryConstructor::toTypedConstructor)

                    val currentClassType = ClassType(
                        it.name.lexeme,
                        null, // todo: superclasses
                        emptyList(), // todo: interfaces
                        mutableMapOf(),
                        mutableMapOf(),
                    )
                    val previousCurrentClass = this.currentClass
                    this.currentClass = currentClassType

                    val classType = VariableType(it.name.lexeme)
                    val classConstructorFunctionType = FunctionType(it.name.lexeme).apply {
                        var generateNoArgs = true
                        primaryConstructor?.let { pc ->
                            if (pc.parameters.isEmpty()) {
                                generateNoArgs = false
                            }

                            addOverload(emptyList(), pc.parameters.map(TypedParameter::type), classType)
                        }

                        secondaryConstructors.forEach { sc ->
                            if (sc.parameters.isEmpty()) {
                                generateNoArgs = false
                            }

                            addOverload(emptyList(), sc.parameters.map(TypedParameter::type), classType)
                        }

                        if (generateNoArgs) {
                            addOverload(emptyList(), emptyList(), classType)
                        }
                    }

                    this.environment.addClass(it.name.lexeme, currentClassType)
                    this.environment.addVariable(
                        it.name.lexeme,
                        classConstructorFunctionType,
                    )

                    this.environment = Environment(this.environment)

                    primaryConstructor?.let { pc ->
                        pc.parameterTypes.forEachIndexed { index, type ->
                            val currParam = pc.parameters[index]
                            when (type) {
                                TypedClassDeclaration.FieldType.VAL, TypedClassDeclaration.FieldType.VAR -> {
                                    this.environment.addVariable(currParam.name.lexeme, currParam.type)
                                    this.currentClass?.addProperty(currParam.name.lexeme, currParam.type)
                                }
                                TypedClassDeclaration.FieldType.NONE -> {}
                            }
                        }
                    }

                    secondaryConstructors.forEach { sc ->
                        val parameterTypes = sc.parameters.map(TypedParameter::type)
                        val argumentTypes = sc.delegatedArguments.map(TypedExpression::type)

                        require(parameterTypes != argumentTypes) {
                            "Cyclic constructor call detected"
                        }

                        val currentConstructorType = FunctionType.Overload(emptyList(), argumentTypes, classType)

                        val constructorOverloads = classConstructorFunctionType.overloads

                        require(currentConstructorType in constructorOverloads) {
                            "Undefined constructor with type $currentConstructorType"
                        }
                    }

                    /*
                    todo:
                     FieldType.NONE parameters inside the primary constructor should be visible within property
                     initializers but not within method bodies
                     */
                    val typedFields = check(it.fields)
                    val typedMethods = check(it.methods)

                    this.environment = this.environment.enclosing!!

                    this.currentClass = previousCurrentClass

                    this.scope = previousScope

                    @Suppress("UNCHECKED_CAST")
                    TypedClassDeclaration(
                        name = it.name,
                        primaryConstructor = primaryConstructor,
                        secondaryConstructors = secondaryConstructors,
                        superClass = superClassType,
                        interfaces = interfaceTypes,
                        fields = typedFields as List<TypedVariableStatement>,
                        methods = typedMethods as List<TypedFunctionDeclaration>,
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
                    val typedParameters = it.parameters.map(Parameter::toTypedParameter)
                    val returnType = it.returnType.toType()

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

                    val contextTypes = it.contexts.map(parser.ast.Type::toType)
                    val parameterTypes = typedParameters.map(TypedParameter::type)

                    val overload = oldFunctionType.addOverload(contextTypes, parameterTypes, returnType)
                    if (this.scope == Scope.CLASS_LEVEL) {
                        this.currentClass!!.addFunction(it.name.lexeme, contextTypes, parameterTypes, returnType)
                    }

                    this.environment = Environment(this.environment)

                    contextTypes.forEach {
                        this.environment.addContextVariable(it)
                    }

                    typedParameters.forEach { (parameterName, parameterType) ->
                        this.environment.addVariable(parameterName.lexeme, parameterType)
                    }

                    val returns = mutableListOf<Type>()

                    val previousScope = this.scope
                    this.scope = Scope.FUNCTION_LEVEL

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
                    this.scope = previousScope

                    // todo: update a better way to handle function overloads
                    TypedFunctionDeclaration(it.name, "$name/${overload.overloadSuffix()}", contextTypes, typedParameters, returnType, typedBody)
                }
                is VariableStatement -> {
                    val type = it.type?.toType() ?: error("Variables must be annotated with a type (type inference is not implemented)")

                    val typedInitializer = it.initializer?.toTypedExpression()
                    val initializerType = typedInitializer?.type

                    initializerType?.let { initType ->
                        if (initType != type) {
                            error("Variable initializer does not match declared type, found $initType but expected $type")
                        }
                    }

                    this.environment.addVariable(it.name.lexeme, type)

                    if (this.scope == Scope.CLASS_LEVEL) {
                        this.currentClass!!.addProperty(it.name.lexeme, type)
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
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
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

                when (calleeType) {
                    is VariableType -> error("Invoke on custom non-function/lambda types are currently not supported")
                    is LambdaType -> {
                        val typedArguments = this.arguments.map {
                            it.toTypedExpression()
                        }

                        /**
                         * todo: handle invocation of contextual lambdas within correct contexts
                         * e.g. the following is correct:
                         *
                         * ```kt
                         * val l: context(Int) (Int) -> Int = { ... }
                         * with(10) {
                         *   l(20)
                         * }
                         * ```
                         *
                         * currently, only way to invoke contextual lambdas is
                         *
                         * ```kt
                         * val l: context(Int) (Int) -> Int = { ... }
                         * l(10, 20)
                         * ```
                         *
                         * though this should only be how the desugared invocation looks
                         */
                        val finalTypedArguments = buildList {
                            var argIndex = 0

                            for (type in calleeType.contextTypes) {
                                if (argIndex !in typedArguments.indices) {
                                    error("Not enough arguments passed to invoke $calleeType")
                                }

                                this@TypeChecker.environment.getContextVariable(type)?.let {
                                    add(it)
                                } ?: run {
                                    val argumentType = typedArguments[argIndex++]

                                    if (type != argumentType.type) {
                                        error("Argument of type ${argumentType.type} does not match expected context type of $type")
                                    } else {
                                        add(argumentType)
                                    }
                                }
                            }

                            for (type in calleeType.parameterTypes) {
                                if (argIndex !in typedArguments.indices) {
                                    error("Not enough arguments passed to invoke $calleeType")
                                }

                                val argumentType = typedArguments[argIndex++]

                                if (type != argumentType.type) {
                                    error("Argument of type ${argumentType.type} does not match $type")
                                } else {
                                    add(argumentType)
                                }
                            }
                        }

                        TypedCall(
                            TypedGet(
                                typedCallee,
                                this.paren.copy(type = TokenType.IDENTIFIER, "invoke"),
                                calleeType,
                            ),
                            this.paren,
                            finalTypedArguments,
                            calleeType.returnType,
                        )
                    }
                    is FunctionType -> {
                        /**
                         * todo: handle invocation of contextual functions within correct contexts
                         * e.g. the following is correct:
                         *
                         * ```kt
                         * context(Int) fun f(x: Int): Int { ... }
                         * with(10) {
                         *   f(20)
                         * }
                         * ```
                         *
                         * however, unlike contextual lambdas, the following will not be supported
                         *
                         * ```kt
                         * context(Int) fun f(x: Int): Int { ... }
                         * f(10, 20)
                         * ```
                         *
                         * since contextual functions should only be callable from within the correct context
                         * unlike contextual lambdas which should(?) be able to introduce contextual values (design question)
                         */
                        val typedArguments = this.arguments.map {
                            it.toTypedExpression()
                        }

                        /**
                         * todo: return back to figure out a solution to give better error diagnostics
                         *
                         * todo: implement overload resolution to choose overload with most contexts (if all contexts exist)
                         *
                         * e.g.
                         *
                         * ```kt
                         * object A
                         * object B
                         *
                         * context(A) fun test() { ... }    // 1
                         * context(A, B) fun test() { ... } // 2
                         *
                         * with(A, B) {
                         *   test() // should call to 2
                         * }
                         * ```
                         *
                         * this means that resolution can not end early (line 490)
                         */
                        var found: FunctionType.Overload? = null
                        var foundArgs: List<TypedExpression> = emptyList()
                        loop@ for (functionOverload in calleeType.overloads) {
                            // todo: update to language version 2.2
                            // as the following cannot be a buildList as non-local break and continue is still experimental
                            val args = mutableListOf<TypedExpression>()

                            for (type in functionOverload.contextTypes) {
                                this@TypeChecker.environment.getContextVariable(type)?.let {
                                    args.add(it)
                                } ?: continue@loop
                            }

                            if (functionOverload.arity != typedArguments.size) {
                                continue // error diagnostic?
                            }
                            for (i in typedArguments.indices) {
                                val argument = typedArguments[i]
                                val type = functionOverload.parameterTypes[i]

                                if (type != argument.type) {
                                    // error("Argument of type ${argumentType.type} does not match $type")
                                    continue@loop
                                } else {
                                    args.add(argument)
                                }
                            }

                            found = functionOverload
                            foundArgs = args
                        }

                        if (found == null) {
                            error("No valid function matching the call signature for ${calleeType.name} was found. Known candidates are: $calleeType")
                        }

                        // todo: find a better way to handle overloads
                        val callee = when (typedCallee) {
                            is TypedVariable -> {
                                typedCallee.copy(
                                    mangledName = "${typedCallee.name.lexeme}/${found.overloadSuffix()}"
                                )
                            }
                            else -> error("Currently only support calling function types from TypedVariable AST")
                        }

                        TypedCall(callee, this.paren, foundArgs, found.returnType)
                    }
                }
            }
            is Get -> {
                val typedInstance = this.instance.toTypedExpression()

                val receiverName = when (val type = typedInstance.type) {
                    is VariableType -> type.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
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

                val typedTrue = check(this.trueBranch)
                val (trueType, typedTrueBranch) = when (val trueBranchLast = typedTrue.lastOrNull()) {
                    is TypedExpressionStatement -> trueBranchLast.expression.let { it.type to (typedTrue.dropLast(1) + TypedReturnExpressionStatement(it)) }
                    else -> VariableType("Unit") to typedTrue // todo: Unit constructor
                }

                val typedFalse = check(this.falseBranch)
                val (falseType, typedFalseBranch) = when (val falseBranchLast = typedFalse.lastOrNull()) {
                    is TypedExpressionStatement -> falseBranchLast.expression.let { it.type to (typedFalse.dropLast(1) + TypedReturnExpressionStatement(it)) }
                    else -> VariableType("Unit") to typedFalse // todo: Unit constructor
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
            is Lambda -> {
                val contextTypes = this.contexts.map(parser.ast.Type::toType)
                val typedParameters = this.parameters.map {
                    TypedLambda.TypedParameter(
                        it.name,
                        it.type?.toType() ?: error("Lambda parameters must be annotated with a type (type inference is not implemented)"),
                    )
                }

                this@TypeChecker.environment = Environment(this@TypeChecker.environment)

                contextTypes.forEach {
                    this@TypeChecker.environment.addContextVariable(it)
                }

                typedParameters.forEach { (parameterName, parameterType) ->
                    this@TypeChecker.environment.addVariable(parameterName.lexeme, parameterType)
                }

                // todo: determine if keeping at function level is ok
                val previousScope = this@TypeChecker.scope
                this@TypeChecker.scope = Scope.FUNCTION_LEVEL

                // todo: update type check to error on using un-labelled return statements in lambdas
                val body = check(this.body)

                val (returnType, typedBody) = when (val trueBranchLast = body.lastOrNull()) {
                    is TypedExpressionStatement -> trueBranchLast.expression.let { it.type to (body.dropLast(1) + TypedReturnExpressionStatement(it)) }
                    else -> VariableType("Unit") to body // todo: Unit constructor
                }

                this@TypeChecker.environment = this@TypeChecker.environment.enclosing!!
                this@TypeChecker.scope = previousScope

                TypedLambda(
                    contextTypes,
                    typedParameters,
                    typedBody,
                    LambdaType(
                        contextTypes,
                        typedParameters.map(TypedLambda.TypedParameter::type),
                        returnType,
                    ),
                )
            }
            is Logical -> {
                val leftTypedExpression = this.left.toTypedExpression()
                val rightTypedExpression = this.right.toTypedExpression()
                val function = this.operator.lexeme

                val leftTypeName = when (val leftType = leftTypedExpression.type) {
                    is VariableType -> leftType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
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
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
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

private fun parser.ast.Type.toType(): Type {
    return when (this) {
        is TConstructor -> VariableType(this.toString())
        is LambdaTypeConstructor -> LambdaType(this.contextTypes.map { it.toType() }, this.parameterTypes.map { it.toType() }, this.returnType.toType())
    }
}