package me.ghostbear.koguma.data.mediaQueryParser

data class Group(val leftType: TokenType, val literals: List<Any?>, val rightType: TokenType)

class Parser(val tokens: List<Token>) {
    private var current = 0

    fun groups(): List<Group> {
        val groups = mutableListOf<Group>()
        while (!isAtEnd()) {
            val advance = advance()

            if (advance.type == TokenType.DOUBLE_LESSER_THAN) {
                val literals = mutableListOf<Any?>()
                while (!check(TokenType.DOUBLE_GREATER_THAN) && !isAtEnd()) {
                    literals.addAll(sentence().also { if (it.isEmpty()) advance() })
                }
                if (check(TokenType.DOUBLE_GREATER_THAN)) {
                    groups.add(
                        Group(
                            advance.type,
                            literals,
                            advance().type
                        )
                    )
                }
            }

            if (advance.type == TokenType.LEFT_DOUBLE_BRACKETS) {
                val literals = mutableListOf<Any?>()
                while (!check(TokenType.RIGHT_DOUBLE_BRACKETS) && !isAtEnd()) {
                    literals.addAll(sentence().also { if (it.isEmpty()) advance() })
                }
                if (check(TokenType.RIGHT_DOUBLE_BRACKETS)) {
                    groups.add(
                        Group(
                            advance.type,
                            literals,
                            advance().type
                        )
                    )
                }
            }

            if (advance.type == TokenType.LEFT_DOUBLE_BRACES) {
                val literals = mutableListOf<Any?>()
                while (!check(TokenType.RIGHT_DOUBLE_BRACES) && !isAtEnd()) {
                    literals.addAll(sentence().also { if (it.isEmpty()) advance() })
                }
                if (check(TokenType.RIGHT_DOUBLE_BRACES)) {
                    groups.add(
                        Group(
                            advance.type,
                            literals,
                            advance().type
                        )
                    )
                }
            }


        }
        return groups
    }

    fun sentence(): MutableList<Any?> {
        val literals = mutableListOf<Any?>()
        while (match(TokenType.STRING, TokenType.NUMBER)) {
            literals.add(previous().literal)
        }
        return literals
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