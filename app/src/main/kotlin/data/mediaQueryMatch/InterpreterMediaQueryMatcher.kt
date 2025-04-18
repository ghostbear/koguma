package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatch
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults

data class InterpreterMediaQueryMatch(
    override val query: String,
    override val type: MediaType
) : MediaQueryMatch

class InterpreterMediaQueryMatcher : MediaQueryMatcher {
    override fun match(query: String): MediaQueryResults {
        val scanner = Scanner(query)
        val tokens = scanner.scanTokens()

        val matches = mutableListOf<MediaQueryMatch>()

        var currentOperator: Token? = null
        var currentGroup = mutableListOf<Token>()
        for (token in tokens) {
            if (currentOperator == null && OPERATOR_START.contains(token.type)) {
                currentOperator = token
                continue
            }
            if (
                currentOperator != null &&
                OPERATOR_END.contains(token.type) &&
                ((currentOperator.type == TokenType.LEFT_DOUBLE_BRACES && token.type == TokenType.RIGHT_DOUBLE_BRACES) ||
                (currentOperator.type == TokenType.LEFT_DOUBLE_BRACKETS && token.type == TokenType.RIGHT_DOUBLE_BRACKETS) ||
                (currentOperator.type == TokenType.DOUBLE_LESSER_THAN && token.type == TokenType.DOUBLE_GREATER_THAN))
                ) {
                matches.add(
                    InterpreterMediaQueryMatch(
                        currentGroup.joinToString(" ") { it.literal?.toString() ?: "" },
                        when (currentOperator.type) {
                            TokenType.LEFT_DOUBLE_BRACES -> MediaType.ANIME
                            TokenType.LEFT_DOUBLE_BRACKETS -> MediaType.NOVEL
                            TokenType.DOUBLE_LESSER_THAN  -> MediaType.MANGA
                            else -> throw IllegalStateException("Unexpected token: $currentOperator")
                        }
                    )
                )
                currentOperator = null
                currentGroup.clear()
                continue
            }
            if (currentOperator != null) {
                currentGroup.add(token)
            }
        }

        return MediaQueryResults(matches)
    }

    companion object {
        val OPERATOR_START = listOf(TokenType.LEFT_BRACKETS, TokenType.LEFT_DOUBLE_BRACKETS, TokenType.LEFT_DOUBLE_BRACES, TokenType.DOUBLE_LESSER_THAN)
        val OPERATOR_END = listOf(TokenType.RIGHT_BRACKETS, TokenType.RIGHT_DOUBLE_BRACKETS, TokenType.RIGHT_DOUBLE_BRACES, TokenType.DOUBLE_GREATER_THAN)
    }
}

enum class TokenType {
    LEFT_DOUBLE_BRACES, RIGHT_DOUBLE_BRACES,
    LEFT_BRACKETS, LEFT_DOUBLE_BRACKETS, RIGHT_BRACKETS, RIGHT_DOUBLE_BRACKETS,
    DOUBLE_LESSER_THAN, DOUBLE_GREATER_THAN,

    STRING, NUMBER,

    EOF
}

data class Token(
    val type: TokenType,
    val value: String? = null,
    val literal: Any? = null,
)

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

    fun scanTokens(): List<Token> {
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