package analysis

import analysis.ast.*
import analysis.ast.FunctionType.*
import analysis.ast.Type
import analysis.ast.TypedClassDeclaration.*
import lexer.Token
import lexer.TokenType
import parser.ast.*
import utils.Quad

// public typealias Environment = Map<String, Set<Type>>

public class TypeChecker(public var environment: Environment) {
    private var currentClass: ClassType? = null
    private var currentCaptures: MutableMap<String, TypedCapture> = mutableMapOf()

    private enum class Scope {
        TOP_LEVEL,
        FUNCTION_LEVEL,
        CLASS_LEVEL,
    }

    private var scope: Scope = Scope.TOP_LEVEL

    public fun check(statements: List<Statement>, returnTypes: MutableList<Type> = mutableListOf()): List<TypedStatement> {
        fun Parameter.toTypedParameter(inline: Boolean = false): TypedParameter {
            var parameterType = this.type.toType()

            if (inline && parameterType is LambdaType) {
                println("[LOG]: setting ${this.name.lexeme} to be an inline lambda type")
                parameterType = parameterType.copy(
                    inline = true
                )
            }

            val typedValue = this.value?.let { v ->
                v.toTypedExpression(parameterType).also { tv ->
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
                    fun ClassDeclaration.PrimaryConstructor.toTypedConstructor(): TypedPrimaryConstructor {
                        return TypedPrimaryConstructor(
                            this.parameters.map(Parameter::toTypedParameter),
                            this.parameterType.map { fieldType ->
                                when (fieldType) {
                                    ClassDeclaration.FieldType.VAL -> FieldType.VAL
                                    ClassDeclaration.FieldType.VAR -> FieldType.VAR
                                    ClassDeclaration.FieldType.NONE -> FieldType.NONE
                                }
                            }
                        )
                    }
                    fun ClassDeclaration.SecondaryConstructor.toTypedConstructor(classType: Type): TypedSecondaryConstructor {
                        this@TypeChecker.environment = Environment(this@TypeChecker.environment, classType)

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

                        return TypedSecondaryConstructor(typedParameters, typedDelegatedArguments, typedBody)
                    }

                    if (this.environment.getClass(it.name.lexeme) != null) {
                        error("Class ${it.name.lexeme} is already defined")
                    }

                    val previousScope = this.scope
                    this.scope = Scope.CLASS_LEVEL

                    val currentClassType = ClassType(
                        it.name.lexeme,
                        null, // todo: superclasses
                        emptyList(), // todo: interfaces
                        mutableMapOf(),
                        mutableMapOf(),
                        false,
                    )

                    // todo: superclasses
                    val superClassType = it.superClass?.let { superClass ->
                        VariableType(superClass.lexeme)
                    }

                    // todo: interfaces
                    val interfaceTypes = it.interfaces.map { i ->
                        VariableType(i.lexeme)
                    }

                    val previousCurrentClass = this.currentClass
                    this.currentClass = currentClassType

                    this.environment.addClass(it.name.lexeme, currentClassType)
                    this.environment.addVariable(
                        it.name.lexeme,
                        currentClassType,
                        // classConstructorFunctionType,
                    )

                    val primaryConstructor = it.primaryConstructor?.toTypedConstructor()

                    primaryConstructor?.let { pc ->
                        pc.parameterTypes.forEachIndexed { index, type ->
                            val currParam = pc.parameters[index]
                            when (type) {
                                FieldType.VAL, FieldType.VAR -> {
                                    // this.environment.addVariable(currParam.name.lexeme, currParam.type)
                                    this.currentClass?.addProperty(currParam.name.lexeme, currParam.type)
                                }
                                FieldType.NONE -> {}
                            }
                        }
                    }

                    val secondaryConstructors = it.secondaryConstructors.map { sc -> sc.toTypedConstructor(currentClassType) }

                    // val classType = VariableType(it.name.lexeme)
                    val classConstructorFunctionType = FunctionType(it.name.lexeme).apply {
                        var generateNoArgs = false
                        primaryConstructor?.let { pc ->
                            // if (pc.parameters.isEmpty()) {
                            //     generateNoArgs = false
                            // }

                            addOverload(null, emptyList(), pc.parameters.map(TypedParameter::type), currentClassType)
                        } ?: run {
                            generateNoArgs = true
                        }

                        secondaryConstructors.forEach { sc ->
                            if (sc.parameters.isEmpty()) {
                                generateNoArgs = false
                            }

                            addOverload(null, emptyList(), sc.parameters.map(TypedParameter::type), currentClassType)
                        }

                        if (generateNoArgs) {
                            addOverload(null, emptyList(), emptyList(), currentClassType)
                        }
                    }

                    currentClassType.addFunction("constructor", classConstructorFunctionType)

                    this.environment = Environment(this.environment, currentClassType)

                    secondaryConstructors.forEach { sc ->
                        val parameterTypes = sc.parameters.map(TypedParameter::type)
                        val argumentTypes = sc.delegatedArguments.map(TypedExpression::type)

                        require(parameterTypes != argumentTypes) {
                            "Cyclic constructor call detected"
                        }

                        val currentConstructorType = Overload(
                            null,
                            emptyList(),
                            argumentTypes,
                            currentClassType,
                            false,
                            false,
                            null,
                            null,
                            null,
                        )

                        val constructorOverloads = classConstructorFunctionType.overloads

                        // println("[LOG | SecondaryConstructors]: currentConstructorType = $currentConstructorType with hashcode ${currentConstructorType.hashCode()}")
                        // println("[LOG | SecondaryConstructors]: constructorOverloads = $constructorOverloads with hashcodes ${constructorOverloads.map { o -> o.hashCode() }}")
                        // println("[LOG | SecondaryConstructors]: currentConstructorType in constructorOverloads = ${currentConstructorType in constructorOverloads}")
                        // println("[LOG | SecondaryConstructors]: constructorOverloads.any { o -> o == currentConstructorType} = ${constructorOverloads.any { o -> o == currentConstructorType}}")

                        // NOTE: cannot rely on `in` operator (contains function) as the values of the set (FunctionType.Overload)
                        //       inherently relies on mutation through the return type which is a ClassType
                        // TODO: figure out how to remove the hashcode depending on the mutable properties inside ClassType
                        // require(currentConstructorType in constructorOverloads) {
                        require(constructorOverloads.any { o -> o == currentConstructorType}) {
                            "Undefined constructor with type $currentConstructorType found in $constructorOverloads"
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
                        type = currentClassType,
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
                    // println("[LOG]: checking function declaration $name with inline = ${it.inline}")
                    val typedParameters = it.parameters.map { param -> param.toTypedParameter(it.inline) }
                    // println("[LOG]: typed parameters are $typedParameters")
                    val returnType = it.returnType.toType()
                    val receiverType = it.receiver?.toType()

                    if (this.scope != Scope.CLASS_LEVEL && it.operator && receiverType == null) {
                        error("top level operator must specify a receiver type")
                    }

                    var oldFunctionType = if (this.scope == Scope.CLASS_LEVEL) {
                        val currentClass = this.currentClass ?: error("inside a class scope but current class is null (should be unreachable)")
                        currentClass.functions[name]?.functionType
                        // TODO("finding the function type of a method not implemented")
                    } else {
                        this.environment.getVariable(name)?.first
                    }

                    if (oldFunctionType == null) {
                        val funcType = FunctionType(name)
                        oldFunctionType = funcType
                        if (this.scope == Scope.CLASS_LEVEL) {
                            // currently do nothing
                        } else {
                            this.environment.addVariable(name, oldFunctionType)
                        }
                    } else {
                        require(oldFunctionType is FunctionType) {
                            "Function overloads cannot shadow variables currently" // todo: update environment to allow for both variables and functions to have the same identifier
                        }
                        // if (this.scope == Scope.CLASS_LEVEL && it.override) {
                        //     // todo: check properly for override in inheritance tree
                        // }
                    }

                    val contextTypes = it.contexts.map(parser.ast.Type::toType)
                    val parameterTypes = typedParameters.map(TypedParameter::type)

                    val overload = oldFunctionType.addOverload(
                        receiverType,
                        contextTypes,
                        parameterTypes,
                        returnType,
                        it.operator,
                    )

                    this.environment = Environment(this.environment, receiverType)

                    contextTypes.forEach {
                        this.environment.addContextVariable(it)
                    }

                    typedParameters.forEach { (parameterName, parameterType) ->
                        this.environment.addVariable(parameterName.lexeme, parameterType)
                    }

                    val returns = mutableListOf<Type>()

                    val previousScope = this.scope
                    this.scope = Scope.FUNCTION_LEVEL
                    val previousCaptures = this.currentCaptures
                    this.currentCaptures = mutableMapOf()

                    val typedBody = check(it.body, returns)

                    // todo: clean up hacky solution to have delete statements bypass return type checking
                    val containsDelete = typedBody.size == 1 && typedBody.first() is TypedDeleteStatement
                    if (!containsDelete) {
                        if (returns.isEmpty() && returnType != VariableType("Unit")) {
                            error("Expected function $name to return $returnType but found Unit")
                        }

                        for (type in returns) {
                            if (type.mangledName != returnType.mangledName) {
                                error("Expected function $name to return $returnType but found $type instead")
                            }
                        }
                    }

                    this.environment = this.environment.enclosing!!
                    this.scope = previousScope
                    val captures = this.currentCaptures
                    this.currentCaptures = (previousCaptures + captures.filter { (name, capture) ->
                        // NOTE: second is local test
                        when (capture) {
                            is TypedContextVariable -> this@TypeChecker.environment.getContextVariable(capture.type)?.second?.not() ?: error("context capture found that is not in parent environment")
                            is TypedVariable -> this@TypeChecker.environment.getVariable(name)?.second?.not() ?: error("capture found that is not in parent environment")
                        }
                    }).toMutableMap()

                    println("[LOG]: function declaration ${it.name.lexeme} captures $captures")

                    val deletionReason = if (containsDelete) {
                        (typedBody.first() as TypedDeleteStatement).reason
                    } else {
                        null
                    }

                    overload.apply {
                        this.isDeleted = containsDelete
                        this.deletionReason = deletionReason
                        this.inlinedBody = typedBody.takeIf { _ -> it.inline }
                        this.inlinedParameterNames = typedParameters.takeIf { _ -> it.inline }
                    }
                    if (this.scope == Scope.CLASS_LEVEL) {
                        this.currentClass!!.addFunction(
                            it.name.lexeme,
                            receiverType,
                            contextTypes,
                            parameterTypes,
                            returnType,
                            it.operator,
                            isDeleted = containsDelete,
                            deletionReason = deletionReason,
                            inlinedBody = typedBody.takeIf { _ -> it.inline },
                            inlinedParameterNames = typedParameters.takeIf { _ -> it.inline },
                        )
                    }

                    // TODO: update to a better way of differentiating between method name mangling and function name mangling
                    val namePrefix = if (this.scope == Scope.CLASS_LEVEL)
                        "${this.currentClass!!.name}_${name}"
                    else
                        name

                    // todo: update a better way to handle function overloads
                    TypedFunctionDeclaration(
                        it.name,
                        "$namePrefix/${overload.overloadSuffix()}",
                        receiverType,
                        contextTypes,
                        typedParameters,
                        returnType,
                        captures.values.toSet(),
                        typedBody,
                        it.inline,
                        deleted = containsDelete
                    )
                }
                is VariableStatement -> {
                    val type = it.type?.toType() ?: error("Variables must be annotated with a type (type inference is not implemented)")

                    val typedInitializer = it.initializer?.toTypedExpression()
                    val initializerType = typedInitializer?.type

                    initializerType?.let { initType ->
                        if (initType is ClassType && initType.mangledName != type.mangledName) {
                            error("Variable initializer ${it.name.lexeme} does not match declared type, found class $initType but expected $type")
                        } else if (initType !is ClassType && initType != type) {
                            error("Variable initializer ${it.name.lexeme} does not match declared type, found $initType but expected $type")
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
                is DeleteStatement -> {
                    val typedReason = it.reason?.toTypedExpression()

                    TypedDeleteStatement(it.keyword, typedReason)
                }
            }
        }
    }

    private fun Expression.toTypedExpression(expectedType: Type? = null): TypedExpression {
        return when (this) {
            is Assign -> {
                val typedAssignment = this.expression.toTypedExpression()
                this@TypeChecker.environment.getVariable(this.name.lexeme)?.let { (variableType, local, global) ->
                    if (typedAssignment.type == variableType) {
                        if (!local && !global) {
                            currentCaptures[this.name.lexeme] = TypedVariable(this.name, variableType)
                        }
                        TypedAssign(this.name, typedAssignment)
                    } else {
                        error("Unable to assign value of type ${typedAssignment.type} to variable ${this.name.lexeme} with type $variableType")
                    }
                } ?: error("Undefined variable ${this.name.lexeme}")
            }
            is Binary -> {
                var leftTypedExpression = this.left.toTypedExpression()
                var rightTypedExpression = this.right.toTypedExpression()

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

                val leftType = leftTypedExpression.type
                val leftTypeName = when (leftType) {
                    is VariableType -> leftType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                    is ClassType -> error("Lookup of class types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                    is ClassType -> error("Lookup of class types is currently not supported during type checking")
                }

                fun findReturnType(l: String, eq: (Type) -> Boolean): Triple<ClassType, ClassType.Function, Type>? {
                    val receiverReference = this@TypeChecker.environment.getClass(l) ?: error("Unknown class '$leftTypeName'")

                    val functionReference = receiverReference.functions[function] ?: error("Unknown function '$function' with receiver type '$leftTypeName'")

                    for (functionOverload in functionReference.functionType.overloads) {
                        // todo: update to check for operator status once operator distinction is added
                        if (functionOverload.arity != 1 || !functionOverload.isOperator) {
                            continue
                        }

                        if (eq(functionOverload.parameterTypes[0])) {
                            return Triple(receiverReference, functionReference, functionOverload.returnType)
                        }
                    }

                    return null
                }

                var potential = findReturnType(leftTypeName) { paramType -> rightType == paramType }

                if (potential == null) {
                    potential = if (leftTypeName == "String" && rightTypeName != "String") {
                        val toStringFunc = this@TypeChecker
                            .environment
                            .getClass(rightTypeName)
                            ?.functions["toString"]
                            ?.takeIf {
                                it.functionType.overloads.size == 1 && it.functionType.overloads.first().arity == 0
                            }
                        if (toStringFunc != null) {
                            rightTypedExpression = TypedCall(
                                TypedGet(
                                    instance = rightTypedExpression,
                                    name = Token(TokenType.IDENTIFIER, "toString", -3, -3),
                                    slot = toStringFunc.slot,
                                    type = toStringFunc.functionType,
                                    callInstance = null,
                                ),
                                paren = Token(TokenType.LEFT_PAREN, "(", -3, -3),
                                arguments = emptyList(),
                                type = leftType,
                                methodInvocation = true,
                            )
                            findReturnType("String") { paramType -> paramType.mangledName == "String" }
                        } else {
                            null
                        }
                    }
                    else if (rightTypeName == "String" && leftTypeName != "String") {
                        val toStringFunc = this@TypeChecker
                            .environment
                            .getClass(leftTypeName)
                            ?.functions["toString"]
                            ?.takeIf {
                                it.functionType.overloads.size == 1 && it.functionType.overloads.first().arity == 0
                            }
                        if (toStringFunc != null) {
                            rightTypedExpression = TypedCall(
                                TypedGet(
                                    instance = leftTypedExpression,
                                    name = Token(TokenType.IDENTIFIER, "toString", -3, -3),
                                    slot = toStringFunc.slot,
                                    type = toStringFunc.functionType,
                                    callInstance = null,
                                ),
                                paren = Token(TokenType.LEFT_PAREN, "(", -3, -3),
                                arguments = emptyList(),
                                type = leftType,
                                methodInvocation = true,
                            )
                            findReturnType("String") { paramType -> paramType.mangledName == "String" }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                if (potential == null) {
                    // error("Unable to find operator function definition on type $leftTypeName for $function with parameter $rightTypeName. Known candidates are: ${functionReference.functionType}")
                    error("Unable to find operator function definition on type $leftTypeName for $function with parameter $rightTypeName.")
                }

                val (receiverReference, functionReference, returnType) = potential

                if (receiverReference.isPrimitive) {
                    TypedBinary(leftTypedExpression, this.operator, rightTypedExpression, returnType)
                } else {
                    TypedCall(
                        callee = TypedGet(
                            instance = leftTypedExpression,
                            name = this.operator.copy(lexeme = function),
                            slot = functionReference.slot,
                            type = functionReference.functionType,
                            callInstance = null,
                        ),
                        paren = this.operator,
                        arguments = listOf(rightTypedExpression),
                        type = returnType,
                        methodInvocation = true
                    )
                }
            }
            is Call -> {
                val typedCallee = this.callee.toTypedExpression()
                val calleeType = typedCallee.type
                val typedPinnedContexts = this.pinnedContexts?.map(parser.ast.Type::toType)

                when (calleeType) {
                    is VariableType -> error("Invoke on custom non-function/lambda types are currently not supported")
                    is LambdaType -> {
                        /**
                         * handles invocation of contextual lambdas within correct contexts
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
                        val inlinedContexts = mutableListOf<Pair<Type, Boolean>>()
                        val finalTypedArguments = buildList {
                            var argIndex = 0

                            if (typedPinnedContexts == null) {
                                for (type in calleeType.contextTypes) {
                                    if (argIndex !in this@toTypedExpression.arguments.indices) {
                                        error("Not enough arguments passed to invoke $calleeType")
                                    }

                                    this@TypeChecker.environment.getContextVariable(type)?.let { (cvar, local) ->
                                        if (!local) {
                                            // todo: figure out how to merge captures with context variables
                                            currentCaptures[cvar.toString()] = cvar
                                        }
                                        add(cvar)
                                        inlinedContexts.add(type to false)
                                    } ?: run {
                                        val typedArgument =
                                            this@toTypedExpression.arguments[argIndex++].toTypedExpression(type)

                                        val initialTypeEq = type == typedArgument.type
                                        val typeNameCheck = type.mangledName == typedArgument.type.mangledName
                                        val typeEq = initialTypeEq || typeNameCheck
                                        val isAny = type.mangledName == "Any"

                                        // check still required as expectedType is ignored (currently) by all other branches
                                        // except Lambda
                                        if (!isAny && !typeEq) {
                                            error("Argument of type ${typedArgument.type} does not match expected context type of $type")
                                        } else {
                                            add(typedArgument)
                                            inlinedContexts.add(type to true)
                                        }
                                    }
                                }
                            } else {
                                // todo: determine if lambda types should be allowed to be called with qualified context call syntax (this can only occur with shadowing of lambda variables)
                                error("Lambda types currently can not be called with qualified context call syntax")
                            }

                            for (type in calleeType.parameterTypes) {
                                if (argIndex !in this@toTypedExpression.arguments.indices) {
                                    error("Not enough arguments passed to invoke $calleeType")
                                }

                                val typedArgument = this@toTypedExpression.arguments[argIndex++].toTypedExpression(type)

                                // check still required as expectedType is ignored (currently) by all other branches
                                // except Lambda
                                if (type != typedArgument.type) {
                                    error("Argument of type ${typedArgument.type} does not match $type")
                                } else {
                                    add(typedArgument)
                                }
                            }
                        }

                        val (callee, args) = if (typedCallee is TypedGet) {
                            TypedVariable(
                                typedCallee.name,
                                typedCallee.type,
                            ) to listOf(typedCallee.instance) + finalTypedArguments
                        } else {
                            TypedGet(
                                typedCallee,
                                this.paren.copy(type = TokenType.IDENTIFIER, "invoke"),
                                -1,
                                calleeType,
                                null
                            ) to finalTypedArguments
                        }

                        if (calleeType.inline) {
                            TypedInlineCall(
                                callee = typedCallee,
                                // TypedGet(
                                //     typedCallee,
                                //     this.paren.copy(type = TokenType.IDENTIFIER, "invoke"),
                                //     calleeType,
                                // ),
                                this.paren,
                                finalTypedArguments,
                                calleeType.returnType,
                                inlinedBody = emptyList(),
                                inlinedParameterNames = emptyList(),
                                inlinedContexts = inlinedContexts.toList(),
                            )
                        } else {
                            TypedCall(
                                callee,
                                this.paren,
                                args,
                                calleeType.returnType,
                                calleeType.receiverType != null, // todo: handle lambdas with receivers
                            )
                        }
                    }
                    is FunctionType -> {
                        /**
                         * handles invocation of contextual functions within correct contexts
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
                         *
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
                        val found = mutableMapOf<Int, MutableList<Pair<Overload, MutableList<TypedExpression>>>>()
                        val typedArgumentsCache = mutableMapOf<Int, TypedExpression>()
                        loop@ for (functionOverload in calleeType.overloads) {
                            // todo: update to language version 2.2
                            // as the following cannot be a buildList as non-local break and continue is still experimental
                            val args = mutableListOf<TypedExpression>()

                            if (typedPinnedContexts == null) {
                                for (type in functionOverload.contextTypes) {
                                    this@TypeChecker.environment.getContextVariable(type)?.let { (cvar, local) ->
                                        if (!local) {
                                            // todo: figure out how to merge captures with context variables
                                            currentCaptures[cvar.toString()] = cvar
                                        }
                                        args.add(cvar)
                                    } ?: continue@loop
                                }
                            } else {
                                // assuming context declarations are de-duplicated (maybe should be enforced by compiler?)
                                // assuming pinned context declaration are de-duplicated (maybe should be enforced by compiler?)
                                if (typedPinnedContexts.size != functionOverload.contextTypes.size) {
                                    continue@loop
                                }

                                for (context in functionOverload.contextTypes) {
                                    if (context !in typedPinnedContexts) {
                                        continue@loop
                                    } else {
                                        this@TypeChecker.environment.getContextVariable(context)?.let { (cvar, local) ->
                                            if (!local) {
                                                // todo: figure out how to merge captures with context variables
                                                currentCaptures[cvar.toString()] = cvar
                                            }
                                            args.add(cvar)
                                        } ?: continue@loop
                                    }
                                }
                            }

                            val numOfContexts = args.size

                            if (functionOverload.arity != this.arguments.size) {
                                continue // error diagnostic?
                            }

                            for (i in this.arguments.indices) {
                                val type = functionOverload.parameterTypes[i]
                                val argument = if (type is LambdaType) {
                                    this.arguments[i].toTypedExpression(type)
                                } else {
                                    typedArgumentsCache.getOrPut(i) {
                                        this.arguments[i].toTypedExpression()
                                    }
                                }

                                val initialTypeEq = type == argument.type
                                val typeNameCheck = type.mangledName == argument.type.mangledName
                                val typeEq = initialTypeEq || typeNameCheck
                                val isAny = type.mangledName == "Any"

                                // check still required as expectedType is ignored (currently) by all other branches
                                // except Lambda
                                if (!isAny && !typeEq) {
                                    // println("[LOG]: function ${calleeType.name} - Argument of type ${argument.type} does not match $type")
                                    continue@loop
                                } else {
                                    args.add(argument)
                                }
                            }

                            found.getOrPut(numOfContexts) {
                                mutableListOf()
                            }.add(functionOverload to args)
                        }

                        if (found.isEmpty()) {
                            error("No valid function matching the call signature for ${calleeType.name}${if (typedPinnedContexts?.isEmpty() ?: true) " " else " with pinned contexts of (${typedPinnedContexts.joinToString(", ")}) "}was found. Known candidates are: $calleeType")
                        }

                        val max = if (typedPinnedContexts == null) {
                            found.maxBy { it.key }.value
                        } else {
                            require(found.size == 1) {
                                "When pinning contexts, only one overload candidate should be found"
                            }
                            found[typedPinnedContexts.size] ?: error("When pinning contexts, the overload found should have the pinned amount of contexts")
                        }

                        // TODO: better handling of subtypes
                        val (foundOverload, foundArgs) = if (max.size != 1) {
                            val noAny = max.filter { (overload, _) ->
                                overload.parameterTypes.all { it.mangledName != "Any" }
                            }
                            if (noAny.size != 1) {
                                error(
                                    "Ambiguous function call for ${calleeType.name}${
                                        if (typedPinnedContexts?.isEmpty() ?: true) " " else " with pinned contexts of (${
                                            typedPinnedContexts.joinToString(
                                                ", "
                                            )
                                        }) "
                                    }was found. Multiple candidates found: ${max.joinToString(", ") { it.first.toString() }}"
                                )
                            } else {
                                noAny.first()
                            }
                        } else {
                            max.first()
                        }

                        if (foundOverload.isDeleted) {
                            error("Calling of deleted signature for ${calleeType.name}${if (typedPinnedContexts?.isEmpty() ?: true) " " else " with pinned contexts of (${typedPinnedContexts.joinToString(", ")}) "}was found. Deletion reason: ${foundOverload.deletionReason?.toString() ?: "none given"}")
                        }

                        // todo: find a better way to handle overloads
                        val (callee, method) = when (typedCallee) {
                            is TypedVariable -> {
                                typedCallee.copy(
                                    mangledName = "${typedCallee.name.lexeme}/${foundOverload.overloadSuffix()}"
                                ) to false
                            }
                            is TypedGet -> {
                                if (typedCallee.callInstance != null) {
                                    foundArgs.add(0, typedCallee.callInstance)
                                    typedCallee to true
                                } else if (typedCallee.slot == -1) { // note: extension
                                    // adding instance to the beginning of args
                                    foundArgs.add(0, typedCallee.instance)
                                    TypedVariable(
                                        name = typedCallee.name.copy(
                                            lexeme = "${typedCallee.name.lexeme}/${foundOverload.overloadSuffix()}",
                                        ),
                                        type = typedCallee.type,
                                    ) to false
                                } else {
                                    typedCallee.copy(
                                        name = typedCallee.name.copy(
                                            lexeme = "${typedCallee.name.lexeme}/${foundOverload.overloadSuffix()}"
                                        )
                                    ) to true
                                }
                            }
                            else -> error("Currently only support calling function types from TypedVariable AST")
                        }

                        val inlinedBody = foundOverload.inlinedBody
                        val inlinedParameterNames = foundOverload.inlinedParameterNames
                        if (inlinedBody != null && inlinedParameterNames != null) {
                            TypedInlineCall(
                                callee,
                                this.paren,
                                foundArgs,
                                foundOverload.returnType,
                                inlinedBody,
                                inlinedParameterNames,
                                foundOverload.contextTypes.map { it to false },
                            )
                        } else {
                            TypedCall(
                                callee,
                                this.paren,
                                foundArgs,
                                foundOverload.returnType,
                                method,
                            )
                        }
                    }
                    is ClassType -> {
                        calleeType.functions["constructor"]?.functionType?.overloads?.let { overloads ->
                            val found = mutableListOf<Pair<Overload, List<TypedExpression>>>()
                            val typedArgumentsCache = mutableMapOf<Int, TypedExpression>()
                            loop@ for (overload in overloads) {
                                val args = mutableListOf<TypedExpression>()

                                if (overload.arity != this.arguments.size) {
                                    continue // error diagnostic?
                                }

                                for (i in this.arguments.indices) {
                                    val type = overload.parameterTypes[i]
                                    val argument = if (type is LambdaType) {
                                        this.arguments[i].toTypedExpression(type)
                                    } else {
                                        typedArgumentsCache.getOrPut(i) {
                                            this.arguments[i].toTypedExpression()
                                        }
                                    }

                                    // check still required as expectedType is ignored (currently) by all other branches
                                    // except Lambda
                                    if (type.mangledName != "Any" && type != argument.type) {
                                        // println("[LOG]: function ${calleeType.name} - Argument of type ${argument.type} does not match $type")
                                        continue@loop
                                    } else {
                                        args.add(argument)
                                    }
                                }

                                found.add(overload to args.toList())
                            }

                            if (found.isEmpty()) {
                                error("No valid function matching the call signature for ${calleeType.name}${if (typedPinnedContexts?.isEmpty() ?: true) " " else " with pinned contexts of (${typedPinnedContexts.joinToString(", ")}) "}was found. Known candidates are: ${overloads}")
                            }

                            // TODO: better handling of subtypes
                            val (foundOverload, foundArgs) = if (found.size != 1) {
                                val noAny = found.filter { (overload, _) ->
                                    overload.parameterTypes.all { it.mangledName != "Any" }
                                }
                                if (noAny.size != 1) {
                                    error(
                                        "Ambiguous function call for ${calleeType.name}${
                                            if (typedPinnedContexts?.isEmpty() ?: true) " " else " with pinned contexts of (${
                                                typedPinnedContexts.joinToString(
                                                    ", "
                                                )
                                            }) "
                                        }was found. Multiple candidates found: ${found.joinToString(", ") { it.first.toString() }}"
                                    )
                                } else {
                                    noAny.first()
                                }
                            } else {
                                found.first()
                            }

                            val callee = when (typedCallee) {
                                is TypedVariable -> {
                                    typedCallee.copy(
                                        mangledName = "${typedCallee.name.lexeme}/${foundOverload.overloadSuffix()}",
                                    )
                                }
                                else -> error("constructor calls must be directly accessed (for now)")
                            }

                            // note: constructors cannot be inline
                            TypedCall(
                                callee,
                                this.paren,
                                foundArgs,
                                foundOverload.returnType,
                                methodInvocation = false,
                            )
                        } ?: error("class $calleeType does not have a constructor (should be unreachable)")
                    }
                }
            }
            is Get -> {
                val typedInstance = this.instance.toTypedExpression()

                val receiverName = when (val type = typedInstance.type) {
                    is VariableType -> type.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                    is ClassType -> {
                        type.name
                        // error("Lookup of class types is currently not supported during type checking")
                    }
                }

                val classRef = this@TypeChecker.environment.getClass(receiverName) ?: error("Unknown class '$receiverName'")

                // todo: new ast node for getting a function?
                val (instance, getType, slot, calledInstance) = classRef.properties[this.name.lexeme]?.let { (_, type, slot) ->
                    Quad(typedInstance, type, slot, null)
                }
                    ?: classRef.functions[this.name.lexeme]?.let { Quad(typedInstance, it.functionType, it.slot, null) }
                    ?: run {
                        val funcType = this@TypeChecker
                            .environment
                            .currentContextVariables()
                            .mapNotNull { cvar ->
                                val c = when (val t = cvar.type) {
                                    is ClassType -> t
                                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                                    is VariableType -> this@TypeChecker.environment.getClass(t.name)
                                        ?: error("Unknown class '${t.name}'")
                                }

                                c.functions[this.name.lexeme]?.let { func ->
                                    val filteredOverloads = func.functionType.overloads.filter {
                                        it.receiverType != null && it.receiverType == typedInstance.type
                                    }

                                    println("[LOG | TypeChecker.toTypedExpression Get]: ${c.name} -> ${name.lexeme} found $filteredOverloads")

                                    if (filteredOverloads.isNotEmpty()) {
                                        cvar to func.copy(
                                            name = func.name,
                                            functionType = func.functionType.copy(
                                                mutableOverloads = filteredOverloads.toMutableSet()
                                            )
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                            .maxByOrNull { (cvar, _) -> cvar.depth }
                        // todo: current workaround for not re-opening class definitions in the environment for extensions
                        val (functionType, instance, slot, calledInstance) = funcType?.let { (cvar, function) ->
                            Quad(function.functionType, cvar, function.slot, typedInstance)
                        } ?: when (val type = this@TypeChecker.environment.getVariable(this.name.lexeme)?.first) {
                            is ClassType -> error("Extension receiver lookup currently cannot handle class types")
                            is FunctionType -> {
                                val filteredOverloads = type.overloads.filter {
                                    it.receiverType != null && it.receiverType == typedInstance.type
                                }

                                val filteredFunctionType = FunctionType(
                                    type.name,
                                    filteredOverloads.toMutableSet(),
                                )

                                Quad(filteredFunctionType, typedInstance, -1, null)
                            }
                            is LambdaType -> {
                                if (type.receiverType?.mangledName != typedInstance.type.mangledName) {
                                    error("Incorrect receiver types: ${type.receiverType} vs ${typedInstance.type}")
                                }

                                Quad(type, typedInstance, -1, null)
                            }
                            is VariableType -> error("Extension receiver lookup currently cannot handle variable types")
                            null -> error("Cannot find function ${this.name.lexeme} with receiver $receiverName")
                        }

                        Quad(instance, functionType, slot, calledInstance)
                    }
                    // ?: error("Unknown property ${this.name.lexeme} on class '$receiverName'")

                TypedGet(instance, this.name, slot, getType, calledInstance)
            }
            is Set -> {
                val typedInstance = this.instance.toTypedExpression()
                val typedExpression = this.expression.toTypedExpression()

                val receiverName = when (val type = typedInstance.type) {
                    is ClassType -> type.name
                    is FunctionType -> error("Set of function type not supported")
                    is LambdaType -> error("Set of lambda type not supported")
                    is VariableType -> type.name
                }

                val classRef = this@TypeChecker.environment.getClass(receiverName) ?: error("Unknown class '$receiverName'")

                // TODO: update kotlin version for named destructuring
                val (_, setType, slot) = classRef.properties[this.name.lexeme] ?: error("Unknown property ${this.name.lexeme} on class '$receiverName'")

                // TODO: handle subtyping
                if (setType != typedExpression.type) {
                    error("${typedExpression.type} does not match the expected type of $setType for ${this.name.lexeme}")
                }

                TypedSet(typedInstance, this.name, typedExpression, slot)
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
            is BooleanLiteral, NullLiteral, is StringLiteral -> TypedLiteral(this as Literal<*>)
            is DoubleLiteral, is IntLiteral, is LongLiteral -> {
                val pinned = this.pinned?.map(parser.ast.Type::toType)
                val lit = TypedLiteral(this as Literal<*>)
                // note: first value in the list should be the maximum depth (due to how we are traversing the environment)
                //       so we don't need to maximize by depth
                val fromReceiver = environment
                    .currentReceivers()
                    .let {
                        if (pinned != null) {
                            it.filter { (type, _) ->
                                type in pinned
                            }
                        } else {
                            it
                        }
                    }
                    .findLiteralFunction(lit.type.mangledName) { it }
                    .firstOrNull()
                    ?.let { (p, functionType, returnType, depth) ->
                        TypedCall(
                            TypedGet(
                                TypedThis(
                                    keyword = Token(TokenType.THIS, "this", -1, -1),
                                    at = null,
                                    label = null,
                                    type = p.first
                                ),
                                name = Token(TokenType.IDENTIFIER, "literal", -1, -1),
                                slot = functionType.slot,
                                type = functionType.functionType,
                                callInstance = lit
                            ),
                            paren = Token(TokenType.LEFT_PAREN, "(", -1, -1),
                            arguments = listOf(lit),
                            type = returnType,
                            methodInvocation = true,
                        ) to depth
                    }
                val contextVariables = environment
                    .currentContextVariables()
                    .let {
                        if (pinned != null) {
                            it.filter { cvar ->
                                cvar.type in pinned
                            }
                        } else {
                            it
                        }
                    }
                val fromContexts = contextVariables
                    .findLiteralFunction(lit.type.mangledName) { (depth, cvar) -> cvar to depth }
                    .firstOrNull()
                    ?.let { (cvar, functionType, returnType, depth) ->
                        TypedCall(
                            callee = TypedGet(
                                instance = cvar,
                                name = Token(TokenType.IDENTIFIER, "literal", -1, -1),
                                slot = functionType.slot,
                                type = functionType.functionType,
                                callInstance = lit
                            ),
                            paren = Token(TokenType.LEFT_PAREN, "(", -1, -1),
                            arguments = listOf(lit),
                            type = returnType,
                            methodInvocation = true
                        ) to depth
                    }
                val fromFunction = contextVariables
                    .topLevelLiteralFunction(lit.type.mangledName, pinned)
                    .firstOrNull()
                    ?.let { (contexts, foundOverload, functionType, returnType) ->
                        TypedCall(
                            callee = TypedVariable(
                                name = Token(TokenType.IDENTIFIER, "literal", -1, -1),
                                type = functionType,
                                mangledName = "literal/${foundOverload.overloadSuffix()}"
                            ),
                            paren = Token(TokenType.LEFT_PAREN, "(", -1, -1),
                            arguments = listOf(lit) + contexts,
                            type = returnType,
                            methodInvocation = false,
                        ) to 0
                    }

                // println("[LOG | TypeChecker.toTypedExpression IntLiteral]:\n\t${fromReceiver}\n\t${fromContexts}")

                val literalOverride = listOfNotNull(fromReceiver, fromContexts, fromFunction)
                    .maxByOrNull {
                        it.second
                    }?.first ?: lit

                literalOverride
            }
            is Lambda -> {
                val inline = if (expectedType == null) {
                    false
                } else {
                    require(expectedType is LambdaType) {
                        "inline propagation is only supported on types with inline declarations in term positions, which is currently only lambdas"
                    }

                    expectedType.inline
                }
                val contextTypes = if (expectedType == null || this.contexts.isNotEmpty()) {
                    this.contexts.map(parser.ast.Type::toType)
                } else {
                    require(expectedType is LambdaType) {
                        "context value propagation is only supported on types with context declarations in term positions, which is currently only lambdas"
                    }

                    require(this.contexts.isEmpty()) {
                        "context value propagation is only allowed when the context declaration is empty (should always be true)"
                    }

                    expectedType.contextTypes
                }

                val typedReceiver = expectedType?.receiverType // TODO(Jaran): lambda expressions cannot explicitly define a receiver currently

                val typedParameters = this.parameters.map {
                    TypedLambda.TypedParameter(
                        it.name,
                        it.type?.toType() ?: error("Lambda parameters must be annotated with a type (type inference is not implemented)"),
                    )
                }

                this@TypeChecker.environment = Environment(this@TypeChecker.environment, typedReceiver) // todo: handle lambdas with receivers

                contextTypes.forEach {
                    this@TypeChecker.environment.addContextVariable(it)
                }

                typedParameters.forEach { (parameterName, parameterType) ->
                    this@TypeChecker.environment.addVariable(parameterName.lexeme, parameterType)
                }

                // todo: determine if keeping at function level is ok
                val previousScope = this@TypeChecker.scope
                this@TypeChecker.scope = Scope.FUNCTION_LEVEL
                val previousCaptures = this@TypeChecker.currentCaptures
                this@TypeChecker.currentCaptures = mutableMapOf()

                // todo: update type check to error on using un-labelled return statements in lambdas
                val body = check(this.body)

                val (returnType, typedBody) = when (val trueBranchLast = body.lastOrNull()) {
                    is TypedExpressionStatement -> trueBranchLast.expression.let { it.type to (body.dropLast(1) + TypedReturnExpressionStatement(it)) }
                    else -> VariableType("Unit") to body // todo: Unit constructor
                }

                this@TypeChecker.environment = this@TypeChecker.environment.enclosing!!
                this@TypeChecker.scope = previousScope
                val captures = this@TypeChecker.currentCaptures
                this@TypeChecker.currentCaptures = (previousCaptures + captures.filter { (name, capture) ->
                    // NOTE: second is local test
                    when (capture) {
                        is TypedContextVariable -> this@TypeChecker.environment.getContextVariable(capture.type)?.second?.not() ?: error("context capture found that is not in parent environment")
                        is TypedVariable -> this@TypeChecker.environment.getVariable(name)?.second?.not() ?: error("capture found that is not in parent environment")
                    }
                }).toMutableMap()

                println("[LOG]: lambda captures $captures")

                TypedLambda(
                    contextTypes,
                    typedReceiver,
                    typedParameters,
                    captures.values.toSet(),
                    typedBody,
                    LambdaType(
                        contextTypes,
                        typedReceiver,
                        typedParameters.map(TypedLambda.TypedParameter::type),
                        returnType,
                        inline,
                        inlinedBody = typedBody.takeIf { _ -> inline },
                        inlinedParameterNames = typedParameters.takeIf { _ -> inline }
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
                    is ClassType -> error("Lookup of class types is currently not supported during type checking")
                }

                val rightType = rightTypedExpression.type
                val rightTypeName = when (rightType) {
                    is VariableType -> rightType.name
                    is LambdaType -> error("Lookup of lambda types is currently not supported during type checking")
                    is FunctionType -> error("Lookup of function types is currently not supported during type checking")
                    is ClassType -> error("Lookup of class types is currently not supported during type checking")
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
                if (this.at != null && this.label != null) {
                    val labelType = this.label.toType()

                    // todo: update to support all possible labels
                    // todo: should qualified this be able to qualify receiver based on type instead of function name?
                    this@TypeChecker
                        .environment
                        .getContextVariable(labelType)
                        ?.let { (cvar, local) ->
                            if (!local) {
                                currentCaptures[cvar.toString()] = TypedVariable(name = this.keyword, type = labelType)
                            }
                            cvar
                        }
                        ?: error("Labeled this could not find $labelType in scope")
                } else {
                    // unqualified this will go to current class
                    // todo: update unqualified this usage to include functions/lambdas with a receiver (extensions)
                    TypedThis(
                        this.keyword,
                        null, // @
                        null, // label
                        this@TypeChecker.environment.getCurrentReceiver() ?: error("Invalid use of 'this' when not inside a scope with a receiver")
                    )
                }
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
                    is ClassType -> error("Lookup of class types is currently not supported during type checking")
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
                this@TypeChecker.environment.getVariable(this.name.lexeme)?.let { (variableType, local, global) ->
                    TypedVariable(this.name, variableType).also {
                        if (!local && !global) {
                            currentCaptures[this.name.lexeme] = it
                        }
                    }
                } ?: run {
                    val currentClass = this@TypeChecker.currentClass
                    val property = currentClass?.properties[this.name.lexeme]
                    val function = currentClass?.functions[this.name.lexeme]

                    if (property == null && function == null) {
                        val contexts = this@TypeChecker.environment.currentContextVariables()

                        var foundInstance: TypedContextVariable? = null
                        var foundSlot: Int = -2
                        var foundType: Type? = null

                        contexts.forEach {
                            val cType = it.type

                            val klass = when (cType) {
                                is ClassType -> cType
                                is FunctionType -> error("context lookup for function type not supported during type checking")
                                is LambdaType -> error("context lookup for lambda type not supported during type checking")
                                is VariableType -> this@TypeChecker.environment.getClass(cType.name) ?: error("Unknown class of context type '${cType.name}'")
                            }

                            println("[LOG | Variable.toTypedExpression]: checking $klass for ${this.name.lexeme}")

                            val property = klass.properties[this.name.lexeme]
                            val function = klass.functions[this.name.lexeme]

                            if (property == null && function == null) {
                                // continue
                            } else if (property != null && function == null) {
                                if (foundInstance != null) {
                                    // TODO: aggregate all found variants to allow for label expressions to distinguish
                                    println("[LOG | Variable.toTypedExpression]: multiple contexts found to have member ${this.name.lexeme}")
                                }
                                foundInstance = it
                                foundSlot = property.slot
                                foundType = property.type
                            } else if (property == null && function != null) {
                                if (foundInstance != null) {
                                    // TODO: aggregate all found variants to allow for label call expressions to distinguish
                                    println("[LOG | Variable.toTypedExpression]: multiple contexts found to have member ${this.name.lexeme}")
                                }
                                foundInstance = it
                                foundSlot = function.slot
                                foundType = function.functionType
                            } else {
                                println("[LOG | Variable.toTypedExpression]: shadowing of a context class property and function found for ${this.name.lexeme}")
                                // continue
                            }
                        }

                        if (foundInstance != null && foundType != null) {
                            println("[LOG | Variable.toTypedExpression]: found ${this.name.lexeme}")
                            TypedGet(
                                instance = foundInstance,
                                name = this.name,
                                slot = foundSlot,
                                type = foundType,
                                null,
                            )
                        } else {
                            null
                        }
                    } else if (property != null && function == null) {
                        TypedGet(
                            instance = TypedThis(
                                keyword = this.name,
                                at = null,
                                label = null,
                                type = currentClass
                            ),
                            name = this.name,
                            slot = property.slot,
                            type = property.type,
                            null,
                        )
                    } else if (property == null && function != null) {
                        TypedGet(
                            instance = TypedThis(
                                keyword = this.name,
                                at = null,
                                label = null,
                                type = currentClass
                            ),
                            name = this.name,
                            slot = -1, // TODO: function slots
                            type = function.functionType,
                            null,
                        )
                    } else { // property != null && function != null
                        error("shadowing of a class property and function found")
                    }
                } ?: error("Undefined variable ${this.name.lexeme}")
            }
        }
    }

    private inline fun <T> List<T>.findLiteralFunction(
        primitiveType: String,
        extractor: (T) -> Pair<Type, Int>
    ): List<Quad<T, ClassType.Function, Type, Int>> = mapNotNull {
        val (recv, depth) = extractor(it)
        val type = when (recv) {
            is ClassType -> recv
            is FunctionType -> TODO()
            is LambdaType -> TODO()
            is VariableType -> environment.getClass(recv.name) ?: error("Unknown class '${recv.name}'")
        }

        type.functions["literal"]?.let { literalFunc ->
            val filteredOverloads = literalFunc.functionType.overloads.filter { overload ->
                (overload.receiverType != null && overload.receiverType.mangledName == primitiveType)
                        && overload.arity == 0
                        && overload.isOperator
            }

            when (val c = filteredOverloads.size) {
                0 -> {
                    println("[LOG | TypeChecker.findLiteralFunction]: no overloads of Int.literal() found in ${type.name}")
                    null
                }
                1 -> {
                    Quad(
                        it,
                        literalFunc.copy(
                            functionType = literalFunc.functionType.copy(
                                mutableOverloads = filteredOverloads.toMutableSet()
                            )
                        ),
                        filteredOverloads.first().returnType,
                        depth
                    )
                }
                else -> {
                    println("[LOG | TypeChecker.findLiteralFunction]: multiple ($c) overloads of Int.literal() found in ${type.name}")
                    null
                }
            }
        }
    }

    private fun List<TypedContextVariable>.topLevelLiteralFunction(
        primitiveType: String,
        pinned: List<Type>?,
    ): List<Quad<List<TypedContextVariable>, Overload, FunctionType, Type>> {
        val (literalFuncType, _, _) = this@TypeChecker.environment.getVariable("literal") ?: return emptyList()
        val contexts = this.groupBy { it.type }
        if (pinned != null) {
            if (pinned.any { it !in contexts}) {
                return emptyList()
            }
        }

        if (literalFuncType is FunctionType) {
            val overloads = literalFuncType.overloads
                .filter { overload ->
                    val valid = (overload.receiverType != null && overload.receiverType.mangledName == primitiveType)
                            && overload.arity == 0
                            && overload.isOperator

                    if (pinned == null) {
                        valid && overload.contextTypes.all { it in contexts }
                    } else {
                        valid && overload.contextTypes.size == pinned.size && overload.contextTypes.all { it in pinned }
                    }
                }
                // .also { overloads -> println("[LOG | TypeChecker.topLevelLiteralFunction]: found overloads -> $overloads") }
                .sortedByDescending { overload ->
                    overload.contextTypes.size
                }

            return overloads.map { overload ->
                Quad(
                    overload.contextTypes.map { type ->
                        contexts[type]!!.maxBy { it.depth }
                    },
                    overload,
                    literalFuncType.copy(
                        mutableOverloads = overloads.toMutableSet()
                    ),
                    overload.returnType,
                )
            }
        } else {
            return emptyList()
        }
    }
}

private fun parser.ast.Type.toType(): Type {
    return when (this) {
        is TConstructor -> VariableType(this.toString())
        is LambdaTypeConstructor -> LambdaType(
            contextTypes = this.contextTypes.map { it.toType() },
            receiverType = this.receiverType?.toType(),
            parameterTypes = this.parameterTypes.map { it.toType() },
            returnType = this.returnType.toType(),
            inline = false,
            null,
            null,
        )
    }
}