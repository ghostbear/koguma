package me.ghostbear.koguma.session

import com.github.benmanes.caffeine.cache.Cache

class CaffeineSessionStore<K : Any, V : Any>(
    val cache: Cache<K, V>
) : SessionStore<K, V> {

    override fun getOrNull(sessionId: K): V? {
        return cache.getIfPresent(sessionId)
    }

    override fun put(sessionId: K, value: V) {
        cache.put(sessionId, value)
    }

    override fun remove(sessionId: K) {
        cache.invalidate(sessionId)
    }
}