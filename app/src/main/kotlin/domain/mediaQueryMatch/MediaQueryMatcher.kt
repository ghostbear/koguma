package me.ghostbear.koguma.domain.mediaQueryMatch

interface MediaQueryMatcher {
    fun match(query: String): MediaQueryResults
}