package parser

import lexer.Token

public class TokenStream(
    private val iterator: Iterator<Token>,
    private var index: Int = 0,
    private val internalBuffer: MutableList<Token> = mutableListOf(),
) {
    public fun mark(): Int {
        return this.index
    }

    public fun restoreTo(index: Int): Token {
        this.index = index
        return this.internalBuffer[this.index - 1]
    }

    public fun next(): Token {
        if (this.index >= this.internalBuffer.size) {
            if (this.iterator.hasNext()) {
                this.internalBuffer.add(this.iterator.next())
            } else {
                throw NoSuchElementException()
            }
        }

        return this.internalBuffer[this.index++]
    }
}