package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults

class InterpreterMediaQueryMatcher : MediaQueryMatcher {
    override fun match(query: String): MediaQueryResults {
        val scanner = Scanner(query)
        val tokens = scanner.scanTokens()
        val expression = Parser(tokens).expression()
        return MediaQueryResults(expression.map { InterpreterMediaQueryMatch(it) })
    }
}
