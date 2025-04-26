package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryResults
import me.ghostbear.koguma.ext.trace

class InterpreterMediaQueryMatcher : MediaQueryMatcher {
    override fun match(query: String): MediaQueryResults = trace("matcher", "match") {
        val scanner = Scanner(query)
        val tokens = scanner.scanTokens()
        val expression = Parser(tokens).expression()
        return MediaQueryResults(expression.map { InterpreterMediaQueryMatch(it) })
    }
}
