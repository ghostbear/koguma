package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults

class InterpreterMediaQueryMatcher : MediaQueryMatcher {
    override fun match(query: String): MediaQueryResults {
        val scanner = Scanner(query)
        val tokens = scanner.scanTokens()
        val groups = Parser(tokens).groups()
        return MediaQueryResults(
            groups.map {
                InterpreterMediaQueryMatch(
                    it.literals.filterNotNull().joinToString(" ") { it.toString() },
                    it.leftType.toMediaType()
                )
            }
        )
    }

    fun TokenType.toMediaType(): MediaType {
        return when (this) {
            TokenType.LEFT_DOUBLE_BRACES -> MediaType.ANIME
            TokenType.LEFT_DOUBLE_BRACKETS -> MediaType.NOVEL
            TokenType.DOUBLE_LESSER_THAN -> MediaType.MANGA
            else -> throw IllegalStateException("Unexpected token: $this")
        }
    }
}
