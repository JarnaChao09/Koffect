package parser

import lexer.Token
import lexer.TokenType
import parser.ast.*

public class Parser(tokenSequence: Sequence<Token>) {
    private val tokens = tokenSequence.iterator()
    private var current: Token = tokens.next()
    private lateinit var previous: Token

    public fun parse(): List<Statement> {
        return buildList {
            while (!this@Parser.isAtEnd()) {
                add(this@Parser.declaration())
            }
        }
    }

    private fun declaration(): Statement {
        return when {
            match(TokenType.CLASS) -> {
                this.classDeclaration()
            }
            match(TokenType.VAL, TokenType.VAR) -> {
                this.variableDeclaration()
            }
            match(TokenType.FUN) -> {
                this.functionDeclaration()
            }
            else -> {
                this.statement()
            }
        }
    }

    private fun classDeclaration(): Statement {
        val name = expect(TokenType.IDENTIFIER, "Expect class name")
        val primaryConstructor = if (match(TokenType.CONSTRUCTOR, TokenType.LEFT_PAREN)) {
            match(TokenType.LEFT_PAREN) // constructor path taken inside match

            val parameters = buildList {
                if (!checkCurrent(TokenType.RIGHT_PAREN)) {
                    do {
                        if (size >= 255) {
                            error("Cannot have more than 255 parameters")
                        }

                        val parameterFieldType = when {
                            match(TokenType.VAL) -> ClassDeclaration.FieldType.VAL
                            match(TokenType.VAR) -> ClassDeclaration.FieldType.VAR
                            else -> ClassDeclaration.FieldType.NONE
                        }

                        val parameterName = expect(TokenType.IDENTIFIER, "Expected parameter name")
                        expect(TokenType.COLON, "Expected type annotation after parameter name")
                        val parameterType = type()

                        val parameterInitialValue = if (match(TokenType.ASSIGN)) {
                            this@Parser.expression()
                        } else {
                            null
                        }

                        add(Parameter(parameterName, parameterType, parameterInitialValue) to parameterFieldType)
                    } while (match(TokenType.COMMA))
                }
            }
            expect(TokenType.RIGHT_PAREN, "Expect class primary constructor to end with right parentheses")
            ClassDeclaration.PrimaryConstructor(parameters.map { it.first }, parameters.map { it.second })
        } else {
            null
        }

        val inherits = buildList {
            if (match(TokenType.COLON)) {
                do {
                    // todo: replace with type parsing
                    add(expect(TokenType.IDENTIFIER, "Expected superclass or interface name"))
                } while (match(TokenType.COMMA))
            }
        }
        // todo: filter out the inheritance list into superclass and interfaces

        expect(TokenType.LEFT_BRACE, "Expected a '{' before a class body")

        val fields = mutableListOf<VariableStatement>()
        val methods = mutableListOf<FunctionDeclaration>()
        val secondaryConstructors = mutableListOf<ClassDeclaration.SecondaryConstructor>()

        while (!checkCurrent(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
            when {
                match(TokenType.FUN) -> {
                    methods += this.functionDeclaration()
                }
                match(TokenType.VAL, TokenType.VAR) -> {
                    fields += this.variableDeclaration()
                }
                match(TokenType.CONSTRUCTOR) -> {
                    expect(TokenType.LEFT_PAREN, "Expected a '(' after constructor declaration")

                    val parameters = this.parameterList()

                    expect(TokenType.COLON, "Expected a delegation to the primary constructor or another secondary constructor")
                    expect(TokenType.THIS, "Expected a delegation to the primary constructor or another secondary constructor")
                    expect(TokenType.LEFT_PAREN, "Expected a delegation to the primary constructor or another secondary constructor")

                    val delegatedConstructorArgs = buildList {
                        if (!this@Parser.checkCurrent(TokenType.RIGHT_PAREN)) {
                            do {
                                if (size >= 255) {
                                    error("Can't have more than 255 arguments to a constructor")
                                }
                                add(this@Parser.expression())
                            } while (this@Parser.match(TokenType.COMMA))
                        }
                    }

                    expect(TokenType.RIGHT_PAREN, "Expect ')' after constructor arguments")

                    val body = when {
                        match(TokenType.LEFT_BRACE) -> buildList {
                            while (!this@Parser.checkCurrent(TokenType.RIGHT_BRACE) && !this@Parser.isAtEnd()) {
                                add(this@Parser.declaration())
                            }

                            this@Parser.expect(TokenType.RIGHT_BRACE, "Expect '}' after a block")
                        }
                        else -> emptyList()
                    }

                    secondaryConstructors.add(ClassDeclaration.SecondaryConstructor(parameters, delegatedConstructorArgs, body))
                }
            }
        }

        expect(TokenType.RIGHT_BRACE, "Expected a '}' after a class body")

        return ClassDeclaration(
            name = name,
            primaryConstructor = primaryConstructor,
            secondaryConstructors = secondaryConstructors,
            superClass = inherits.firstOrNull(),
            interfaces = inherits,
            fields = fields,
            methods = methods,
        )
    }

    private fun variableDeclaration(): VariableStatement {
        val type = this.previous.type
        val name = expect(TokenType.IDENTIFIER, "Expected a variable name")

        val typeAnnotation = if (match(TokenType.COLON)) {
            expect(TokenType.IDENTIFIER, "Expected valid type identifier")
        } else {
            null
        }

        val initializer = if (match(TokenType.ASSIGN)) {
            this.expression()
        } else {
            null
        }

        expect(TokenType.EOS, "Expected an end of statement after variable declaration")

        return VariableStatement(type, name, typeAnnotation, initializer)
    }

    private fun functionDeclaration(): FunctionDeclaration {
        val name = expect(TokenType.IDENTIFIER, "Expect function name")
        expect(TokenType.LEFT_PAREN, "Expect '(' after function name")
        val parameters = this.parameterList()

        val returnType = when {
            match(TokenType.COLON) -> {
                this.advance()
                this.previous
            }
            else -> this.previous.copy(type = TokenType.IDENTIFIER, lexeme = "Unit")
        }


        val body = when {
            match(TokenType.LEFT_BRACE) -> buildList {
                while (!this@Parser.checkCurrent(TokenType.RIGHT_BRACE) && !this@Parser.isAtEnd()) {
                    add(this@Parser.declaration())
                }

                this@Parser.expect(TokenType.RIGHT_BRACE, "Expect '}' after a block")
            }
            match(TokenType.ASSIGN) -> listOf(this.statement())
            else -> error("Expected a function body")
        }

        return FunctionDeclaration(name, parameters, returnType, body)
    }

    private fun parameterList(): List<Parameter> {
        val ret = buildList {
            if (!this@Parser.checkCurrent(TokenType.RIGHT_PAREN)) {
                do {
                    if (size >= 255) {
                        error("Cannot have more than 255 parameters")
                    }

                    val parameterName = expect(TokenType.IDENTIFIER, "Expected parameter name")
                    expect(TokenType.COLON, "Expected type annotation after parameter name")
                    val parameterType = type()

                    val parameterInitialValue = if (match(TokenType.ASSIGN)) {
                        this@Parser.expression()
                    } else {
                        null
                    }

                    add(Parameter(parameterName, parameterType, parameterInitialValue))
                } while (match(TokenType.COMMA))
            }
        }
        expect(TokenType.RIGHT_PAREN, "Expected ')' after parameter list")

        return ret
    }

    private fun type(): Type {
        if (match(TokenType.IDENTIFIER)) {
            return TConstructor(this.previous.lexeme)
        } else if (match(TokenType.LEFT_PAREN)) {
            // parenthesized type (A) or function type (A, B) -> C

            val types = if (match(TokenType.RIGHT_PAREN)) {
                emptyList<Type>()
            } else {
                buildList {
                    do {
                        add(type())
                    } while (match(TokenType.COMMA))
                }.also {
                    expect(TokenType.RIGHT_PAREN, "Expected ')' after type${if (it.size > 1) " list" else ""}")
                }
            }

            if (types.size == 1 && this.peek().type != TokenType.ARROW) {
                // parenthesized type (A)
                return types.first()
            }

            expect(TokenType.ARROW, "Expected '->' to specify return type of a function type")

            val returnType = type()

            return TConstructor("Function${types.size}", types + returnType)
        } else {
            error("Expected a type")
        }
    }

    private fun statement(): Statement {
        return when {
            match(TokenType.IF) -> {
                val (condition, trueBranch, falseBranch) = this.generalIf(false)
                IfStatement(condition, trueBranch, falseBranch)
            }
            match(TokenType.WHILE) -> {
                this.whileStatement()
            }
            match(TokenType.RETURN) -> {
                this.returnStatement()
            }
            else -> ExpressionStatement(this.expression()).also {
                expect(TokenType.EOS, "Must end with an end of statement")
            }
        }
    }

    private fun whileStatement(): Statement {
        expect(TokenType.LEFT_PAREN, "Expect '(' at start of while condition")

        val condition = this.expression()

        expect(TokenType.RIGHT_PAREN, "Expect ')' at the end of while condition")

        val body = this.parseBody()

        return WhileStatement(condition, body)
    }

    private fun returnStatement(): Statement {
        val keyword = this.previous

        val expression = if (!checkCurrent(TokenType.EOS)) {
            this.expression()
        } else {
            null
        }

        expect(TokenType.EOS, "Expect an end of statement after return value")

        return ReturnStatement(keyword, expression)
    }

    private fun generalIf(forceTrailingElse: Boolean): Triple<Expression, List<Statement>, List<Statement>> {
        fun parseBranch(): List<Statement> {
            return this.parseBody()
        }
        expect(TokenType.LEFT_PAREN, "Expecting '(' at start of if condition")
        val condition = this.expression()
        expect(TokenType.RIGHT_PAREN, "Expecting ')' at end of if condition")
        val trueBranch = parseBranch()
        val falseBranch = if (forceTrailingElse) {
            expect(TokenType.ELSE, "Expecting if to be followed by else to be used as expression")
            parseBranch()
        } else {
            if (match(TokenType.ELSE)) {
                parseBranch()
            } else {
                listOf()
            }
        }

        return Triple(condition, trueBranch, falseBranch)
    }

    private fun parseBody(): List<Statement> {
        return if (match(TokenType.LEFT_BRACE)) {
            buildList {
                while (!this@Parser.checkCurrent(TokenType.RIGHT_BRACE) && !this@Parser.isAtEnd()) {
                    add(this@Parser.declaration())
                }

                this@Parser.expect(TokenType.RIGHT_BRACE, "Expect '}' after a block")
            }
        } else {
            listOf(this.statement())
        }
    }

    private fun expression(): Expression {
        return this.assignment()
    }

    private fun assignment(): Expression {
        val expr = this.or()

        if (match(TokenType.ASSIGN)) {
            val equals = this.previous
            val value = this.assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            }

            error("Invalid Assignment Target on ${equals.line} at ${equals.column}")
        }

        return expr
    }

    private fun or(): Expression {
        var expr = this.and()

        while (match(TokenType.OR)) {
            val operator = this.previous
            val right = this.and()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): Expression {
        var expr = this.equality()

        while (match(TokenType.AND)) {
            val operator = this.previous
            val right = this.equality()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun equality(): Expression {
        var expr = this.comparison()

        while (match(TokenType.NOT_EQ, TokenType.EQUALS)) {
            val operator = this.previous
            val right = this.comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expression {
        var expr = this.term()

        while (match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            val operator = this.previous
            val right = this.term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expression {
        var expr = this.factor()

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = this.previous
            val right = this.factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expression {
        var expr = this.unary()

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            val operator = this.previous
            val right = this.unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            val operator = this.previous
            return Unary(operator, this.unary())
        }

        return this.call()
    }

    private fun call(): Expression {
        var expr = this.atom()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = this.finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = expect(TokenType.IDENTIFIER, "Expected property name after '.'.")
                expr = Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expression): Expression {
        // todo: named arguments
        val arguments = buildList {
            if (!this@Parser.checkCurrent(TokenType.RIGHT_PAREN)) {
                do {
                    if (size >= 255) {
                        error("Can't have more than 255 arguments")
                    }
                    add(this@Parser.expression())
                } while (this@Parser.match(TokenType.COMMA))
            }
        }

        return Call(callee, expect(TokenType.RIGHT_PAREN, "Expect ')' after arguments"), arguments)
    }

    private fun atom(): Expression {
        return when {
            match(TokenType.TRUE) -> Literal(true)
            match(TokenType.FALSE) -> Literal(false)
            match(TokenType.NULL) -> Literal(null)
            // match(TokenType.SUPER) -> {
            //     val keyword = this.previous
            //     expect(TokenType.DOT, "Expect '.' after 'super'.")
            //     val method = expect(TokenType.IDENTIFIER, "Expect superclass method name")
            //     Super(keyword, method)
            // }
            match(TokenType.THIS) -> This(this.previous)
            match(TokenType.IDENTIFIER) -> Variable(this.previous)
            match(TokenType.NUMBER, TokenType.STRING) -> Literal(this.previous.literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = this.expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' after expression")
                Grouping(expr)
            }
            match(TokenType.IF) -> {
                val (condition, trueBranch, falseBranch) = this.generalIf(true)
                IfExpression(condition, trueBranch, falseBranch)
            }

            else -> error("Invalid expression")
        }
    }

    private fun isAtEnd(): Boolean {
        return this.peek().type == TokenType.EOF
    }

    private fun advance(): Token {
        if (!this.isAtEnd()) {
            this.previous = this.current
            this.current = this.tokens.next()
        }

        return this.previous
    }

    private fun peek(): Token {
        return this.current
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (this.checkCurrent(type)) {
                this.advance()
                return true
            }
        }

        return false
    }

    private fun checkCurrent(type: TokenType): Boolean {
        return !this.isAtEnd() && this.peek().type == type
    }

    private fun expect(type: TokenType, message: String): Token {
        return if (this.checkCurrent(type)) {
            this.advance()
        } else {
            error(message)
        }
    }
}