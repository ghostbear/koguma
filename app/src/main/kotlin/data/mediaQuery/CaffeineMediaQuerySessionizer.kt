package me.ghostbear.koguma.data.mediaQuery

import com.github.benmanes.caffeine.cache.Cache
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaQuerySessionizer

class CaffeineMediaQuerySessionizer<T : Any>(
    val cache: Cache<T, MediaQuery>
) : MediaQuerySessionizer<T> {

    override fun getOrNull(sessionId: T): MediaQuery? {
        return cache.getIfPresent(sessionId)
    }

    override fun put(sessionId: T, mediaQuery: MediaQuery) {
        cache.put(sessionId, mediaQuery)
    }
}