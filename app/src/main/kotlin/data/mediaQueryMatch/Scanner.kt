package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.ext.trace

class Scanner(
    private val source: String,
) {
    private val tokens: MutableList<Token> = mutableListOf()

    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun addToken(type: TokenType) {
        tokens.add(Token(type))
    }

    fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal))
    }

    fun scanTokens(): List<Token> = trace("scanner", "scan_tokens") {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }

    fun scanToken() {
        val c = advance()
        when (c) {
            '<' -> operator('<', TokenType.DOUBLE_LESSER_THAN)
            '>' -> operator('>', TokenType.DOUBLE_GREATER_THAN)
            '{' -> operator('{', TokenType.LEFT_DOUBLE_BRACES)
            '}' -> operator('}', TokenType.RIGHT_DOUBLE_BRACES)
            '[' -> operator('[', TokenType.LEFT_DOUBLE_BRACKETS, TokenType.LEFT_BRACKETS)
            ']' -> operator(']', TokenType.RIGHT_DOUBLE_BRACKETS, TokenType.RIGHT_BRACKETS)
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            else -> {
                if (isDigit(c))
                    number()
                else
                string()
            }
        }
    }

    fun operator(sample: Char, twoCharTokenType: TokenType, oneCharTokenType: TokenType? = null) {
        val match = match(sample)
        when {
            match && (peek() == sample) -> string(WHITESPACES)
            match -> addToken(twoCharTokenType)
            oneCharTokenType == null -> string(WHITESPACES)
            else -> addToken(oneCharTokenType)
        }
    }

    fun isAtEnd(): Boolean {
        return current >= source.length
    }

    fun advance(): Char {
        return source[current++]
    }

    fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false

        current++
        return true
    }

    fun peek(ahead: Int = 0): Char {
        if (isAtEnd()) return '\u0000';
        return source[current + ahead]
    }

    fun isDigit(sample: Char): Boolean {
        return sample >= '0' && sample <= '9'
    }

    fun number() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peek(1))) {
            advance()
            while (isDigit(peek())) advance()
        }

        val value = source.substring(start, current)
        addToken(TokenType.NUMBER, value.toDouble())
    }

    fun string(terminator: List<Char>? = null) {
        val terminator = terminator ?: STRING_TERMINATORS

        while (!terminator.contains(peek()) && !isAtEnd()) {
            advance()
        }

        val value = source.substring(start, current)
        addToken(TokenType.STRING, value)
    }

    companion object {
        val WHITESPACES = listOf(
            ' ',
            '\r',
            '\t',
            '\n',
        )

        val STRING_TERMINATORS = listOf(
            ' ',
            '\r',
            '\t',
            '\n',
            '{',
            '}',
            '<',
            '>',
            '[',
            ']',
        )
    }
}