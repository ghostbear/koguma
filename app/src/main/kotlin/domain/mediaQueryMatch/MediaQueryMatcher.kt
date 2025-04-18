package me.ghostbear.koguma.domain.mediaQueryParser

interface MediaQueryMatcher {
    fun match(query: String): MediaQueryResults
}