package parser

import lexer.Token
import parser.ast.Statement

public class Parser(tokenSequence: Sequence<Token>) {
    private val tokens = tokenSequence.iterator()

    public fun parse(): List<Statement> {
        TODO()
    }
}