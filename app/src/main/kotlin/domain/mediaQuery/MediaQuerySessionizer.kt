package me.ghostbear.koguma.domain.mediaQuery

interface MediaQuerySessionizer<T> {

    fun getOrNull(sessionId: T): MediaQuery?

    fun put(sessionId: T, mediaQuery: MediaQuery)

}