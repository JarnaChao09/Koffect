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
        return if (match(TokenType.VAL, TokenType.VAR)) {
            this.variableDeclaration()
        } else {
            this.statement()
        }
    }

    private fun variableDeclaration(): Statement {
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

        return Variable(type, name, typeAnnotation, initializer)
    }

    private fun statement(): Statement {
        return ExpressionStatement(this.expression()).also {
            expect(TokenType.EOS, "Must end with an end of statement")
        }
    }

    private fun expression(): Expression {
        return this.or()
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

        while (this.match(TokenType.NOT_EQ, TokenType.EQUALS)) {
            val operator = this.previous
            val right = this.comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): Expression {
        var expr = this.term()

        while (this.match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            val operator = this.previous
            val right = this.term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expression {
        var expr = this.factor()

        while (this.match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = this.previous
            val right = this.factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expression {
        var expr = this.unary()

        while (this.match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            val operator = this.previous
            val right = this.unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expression {
        if (this.match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            val operator = this.previous
            return Unary(operator, this.unary())
        }

        return this.atom()
    }

    private fun atom(): Expression {
        return when {
            match(TokenType.TRUE) -> Literal(true)
            match(TokenType.FALSE) -> Literal(false)
            match(TokenType.NULL) -> Literal(null)
//            match(TokenType.SUPER) -> {
//                val keyword = this.previous
//                expect(TokenType.DOT, "Expect '.' after 'super'.")
//                val method = expect(TokenType.IDENTIFIER, "Expect superclass method name")
//                Super(keyword, method)
//            }
//            match(TokenType.THIS) -> This(this.previous())
//            match(TokenType.IDENTIFIER) -> Variable(this.previous())
            match(TokenType.NUMBER, TokenType.STRING) -> Literal(this.previous.literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = this.expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' after expression")
                Grouping(expr)
            }
            match(TokenType.IF) -> {
                expect(TokenType.LEFT_PAREN, "Expecting '(' at start of if expression condition")
                val condition = this.expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' at end of if expression condition")
                val trueBranch = this.expression()
                expect(TokenType.ELSE, "Expecting if to be followed by else to be used as expression")
                val falseBranch = this.expression()

                If(condition, trueBranch, falseBranch)
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
            advance()
        } else {
            error(message)
        }
    }
}