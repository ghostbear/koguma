package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.ext.trace

interface Expr {
    data class Group(val leftType: TokenType, val literals: List<Literal?>, val rightType: TokenType) : Expr
    data class Index(val literal: Literal?) : Expr
    data class Binary(val left: Expr, val right: Expr) : Expr
    data class Literal(val literal: Any?) : Expr
}

fun TokenType.opposite(): TokenType {
    return when (this) {
        TokenType.LEFT_DOUBLE_BRACES -> TokenType.RIGHT_DOUBLE_BRACES
        TokenType.RIGHT_DOUBLE_BRACES -> TokenType.EOF
        TokenType.LEFT_BRACKET -> TokenType.RIGHT_BRACKET
        TokenType.LEFT_DOUBLE_BRACKETS -> TokenType.RIGHT_DOUBLE_BRACKETS
        TokenType.RIGHT_BRACKET -> TokenType.EOF
        TokenType.RIGHT_DOUBLE_BRACKETS -> TokenType.EOF
        TokenType.DOUBLE_LEFT_ANGLE_BRACKETS -> TokenType.DOUBLE_RIGHT_ANGLE_BRACKETS
        TokenType.DOUBLE_RIGHT_ANGLE_BRACKETS -> TokenType.EOF
        TokenType.STRING -> TokenType.EOF
        TokenType.NUMBER -> TokenType.EOF
        TokenType.EOF -> TokenType.EOF
        TokenType.SPACE -> TokenType.EOF
    }
}

class Parser(val tokens: List<Token>) {
    private var current = 0

    fun expression(): List<Expr> = trace("parser", "expression") {
        val expressions = mutableListOf<Expr>()
        while (!isAtEnd()) {
            if (!match(TokenType.DOUBLE_LEFT_ANGLE_BRACKETS, TokenType.LEFT_DOUBLE_BRACKETS, TokenType.LEFT_DOUBLE_BRACES)) {
                advance()
                continue
            }
            val start = previous()

            val literals = mutableListOf<Expr.Literal?>()
            while (!match(start.type.opposite())) {
                val literal = advance()
                literals.add(Expr.Literal(literal.literal))
            }
            val end = previous()

            var expr: Expr = Expr.Group(start.type, literals, end.type)

            val optionalStart = peek()
            if (optionalStart.type == TokenType.LEFT_BRACKET) {
                advance()
                if (peek().type == TokenType.NUMBER) {
                    val number = advance()
                    expr = Expr.Binary(expr, Expr.Index(Expr.Literal(number.literal)))
                }
                while (!match(optionalStart.type.opposite())) {
                }
            }

            expressions.add(expr)
        }
        return expressions
    }

    fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    fun advance(): Token {
        if (!isAtEnd()) current++;
        return previous()
    }

    fun previous(): Token {
        return tokens[current - 1]
    }

    fun peek(): Token {
        return tokens[current]
    }

    fun isAtEnd(): Boolean {
        return current >= tokens.size
    }

}