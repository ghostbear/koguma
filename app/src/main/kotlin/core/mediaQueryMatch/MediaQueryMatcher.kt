package me.ghostbear.koguma.core.mediaQueryMatch

interface MediaQueryMatcher {
    fun match(query: String): MediaQueryResults
}