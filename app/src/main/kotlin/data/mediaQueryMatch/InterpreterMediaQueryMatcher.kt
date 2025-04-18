package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatch
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults

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
