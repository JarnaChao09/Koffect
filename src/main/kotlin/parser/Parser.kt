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
                add(this@Parser.statement())
            }
        }
    }

    private fun statement(): Statement {
        val ret = this.expression()
        expect(TokenType.EOS, "Must end with an end of statement")
        return ExpressionStatement(ret)
    }

    private fun expression(): Expression {
        return term()
    }

    private fun term(): Expression {
        var expr = factor()

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = this.previous
            val right = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expression {
        var expr = unary()

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            val operator = this.previous
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            val operator = this.previous
            return Unary(operator, unary())
        }

        return atom()
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
                val expr = expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' after expression")
                Grouping(expr)
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