package lexer

public data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int,
    val literal: Any? = null
)

public enum class TokenType {
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    COMMA,
    DOT,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    MOD,

    EOS,

    COLON,

    VAR,
    VAL,
    ASSIGN,

    NOT,
    NOT_EQ,
    EQUALS,
    GT,
    GE,
    LT,
    LE,

    TRUE,
    FALSE,
    AND,
    OR,

    BIT_AND,
    BIT_OR,

    IDENTIFIER,
    STRING,
    NUMBER,
    NULL,

    IF,
    ELSE,

    FOR,
    WHILE,

    CONTEXT,
    SUSPEND,
    FUN,
    RETURN,

    CLASS,
    CONSTRUCTOR,
    ABSTRACT,
    INTERFACE,
    THIS,
    SUPER,

    EOF,

    ERROR,
}