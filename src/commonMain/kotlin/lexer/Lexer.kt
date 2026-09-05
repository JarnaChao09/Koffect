package lexer

private val defaultKeywords: Map<String, TokenType> = mapOf(
    "var" to TokenType.VAR,
    "val" to TokenType.VAL,
    "true" to TokenType.TRUE,
    "false" to TokenType.FALSE,
    "null" to TokenType.NULL,
    "if" to TokenType.IF,
    "else" to TokenType.ELSE,
    "for" to TokenType.FOR,
    "while" to TokenType.WHILE,
    "context" to TokenType.CONTEXT,
    "operator" to TokenType.OPERATOR,
    "delete" to TokenType.DELETE,
    "suspend" to TokenType.SUSPEND,
    "inline" to TokenType.INLINE,
    "override" to TokenType.OVERRIDE,
    "fun" to TokenType.FUN,
    "return" to TokenType.RETURN,
    "class" to TokenType.CLASS,
    "constructor" to TokenType.CONSTRUCTOR,
    "abstract" to TokenType.ABSTRACT,
    "interface" to TokenType.INTERFACE,
    "this" to TokenType.THIS,
    "super" to TokenType.SUPER,
)

public class Lexer(private val source: String, private val keywords: Map<String, TokenType> = defaultKeywords) {
    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1
    private var column: Int = 0

    private val interpStack: ArrayDeque<Int> = ArrayDeque()

    public val tokens: Sequence<Token> by lazy {
        sequence {
            var last: Token
            do {
                last = this@Lexer.nextToken()
                yield(last)
            } while (last.type != TokenType.EOF)
        }
    }

    private fun nextToken(): Token {
        this.skipWhitespace()

        if (this.isAtEnd()) {
            return Token(TokenType.EOF, "", line, column)
        }

        this.start = this.current

        return when (val char = this.advance()) {
            '(' -> this.createToken(TokenType.LEFT_PAREN)
            ')' -> this.createToken(TokenType.RIGHT_PAREN)
            '{' -> {
                if (this.interpStack.isNotEmpty()) {
                    this.interpStack.addFirst(this.interpStack.removeFirst() + 1)
                }
                this.createToken(TokenType.LEFT_BRACE)
            }
            '}' -> {
                val tok = if (this.interpStack.isNotEmpty()) {
                    val curr = this.interpStack.removeFirst() - 1
                    if (curr == 0) {
                        this.createString()
                    } else {
                        this.interpStack.addFirst(curr)
                        this.createToken(TokenType.RIGHT_BRACE)
                    }
                } else {
                    this.createToken(TokenType.RIGHT_BRACE)
                }

                tok
            }
            '[' -> this.createToken(TokenType.LEFT_BRACKET)
            ']' -> this.createToken(TokenType.RIGHT_BRACKET)
            ',' -> this.createToken(TokenType.COMMA)
            '.' -> this.createToken(TokenType.DOT)
            ';' -> this.createToken(TokenType.EOS)
            ':' -> this.createToken(TokenType.COLON)
            '@' -> this.createToken(TokenType.AT)
//            '\n' -> Token(TokenType.EOS, "\\n", this.line++, this.column.also {
//                this.column = 0
//            })
            '+' -> this.createToken(TokenType.PLUS)
            '-' -> this.createToken(if (this.match('>')) TokenType.ARROW else TokenType.MINUS)
            '*' -> this.createToken(TokenType.STAR)
            '/' -> this.createToken(TokenType.SLASH)
            '%' -> this.createToken(TokenType.MOD)
            '!' -> this.createToken(if (this.match('=')) TokenType.NOT_EQ else TokenType.NOT)
            '=' -> this.createToken(if (this.match('=')) TokenType.EQUALS else TokenType.ASSIGN)
            '>' -> this.createToken(if (this.match('=')) TokenType.GE else TokenType.GT)
            '<' -> this.createToken(if (this.match('=')) TokenType.LE else TokenType.LT)
            '&' -> this.createToken(if (this.match('&')) TokenType.AND else TokenType.BIT_AND)
            '|' -> this.createToken(if (this.match('|')) TokenType.OR else TokenType.BIT_OR)
            '"' -> this.createString()
            in '0'..'9' -> this.createNumber()
            in 'a'..'z', in 'A'..'Z' -> this.createIdentifier()
            else -> Token(TokenType.ERROR, "Unexpected character $char", line, column)
        }
    }

    private fun skipWhitespace() {
        while (true) {
            when (this.peek()) {
                ' ', '\r' -> this.advance()
                '\t' -> {
                    this.column += 3
                    this.advance()
                }

                '/' -> {
                    if (this.peek(1) == '/') {
                        while (this.peek() != '\n' && !this.isAtEnd()) {
                            this.advance()
                        }
                    } else {
                        return
                    }
                }

                '\n' -> {
                    this.line++
                    this.column = 0
                    this.advance()
                }

                else -> return
            }
        }
    }

    private fun advance(): Char {
        this.column++
        return this.source[this.current++]
    }

    private fun isAtEnd(dist: Int = 0): Boolean = this.current + dist >= this.source.length

    private fun peek(dist: Int = 0): Char = if (this.isAtEnd(dist)) '\u0000' else this.source[this.current + dist]

    private fun createToken(type: TokenType, literal: Any? = null): Token =
        Token(type, this.source.substring(this.start..<this.current), this.line, this.column, literal)

    private fun match(expected: Char): Boolean = if (this.isAtEnd() || this.source[this.current] != expected) {
        false
    } else {
        this.current++
        this.column++
        true
    }

    private fun createString(): Token {
        while (this.peek() != '"' && !this.isAtEnd()) {
            if (this.peek() == '\n') {
                this.line++
                this.column = 0
            }

            if (this.peek() == '$' && this.peek(1) == '{') {
                val token = this.createToken(
                    TokenType.STRING_INTERPOLATION,
                    literal = this.source.substring((this.start + 1)..<this.current)
                )
                this.interpStack.addFirst(1)

                this.advance() // $
                this.advance() // {

                return token
            }

            this.advance()
        }

        if (this.isAtEnd()) {
            return Token(TokenType.ERROR, "Unterminated String", line, column)
        }

        this.advance()

        return this.createToken(
            TokenType.STRING,
            literal = this.source.substring((this.start + 1)..<(this.current - 1))
        )
    }

    private fun createNumber(): Token {
        var conversion: String.() -> Number = String::toInt

        while (this.peek() in '0'..'9') {
            this.advance()
        }

        if (this.peek() == '.' && this.peek(1) in '0'..'9') {
            conversion = String::toDouble
            this.advance()
        }

        while (this.peek() in '0'..'9') {
            this.advance()
        }

        return when (this.peek()) {
            'l', 'L' -> {
                conversion = String::toLong
                this.createToken(
                    TokenType.NUMBER,
                    literal = this.source.substring(this.start..<this.current).conversion()
                ).also {
                    this.advance()
                }
            }
            else -> {
                this.createToken(
                    TokenType.NUMBER,
                    literal = this.source.substring(this.start..<this.current).conversion()
                )
            }
        }
    }

    private fun createIdentifier(): Token {
        while (this.peek().isAlphaNumeric()) {
            this.advance()
        }

        return this.createToken(
            this.keywords.getOrElse(
                this.source.substring(this.start..<this.current)
            ) {
                TokenType.IDENTIFIER
            }
        )
    }

    private fun Char.isAlphaNumeric(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this == '_' || this in '0'..'9'
}