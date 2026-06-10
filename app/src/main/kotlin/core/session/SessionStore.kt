package me.ghostbear.koguma.core.session

interface SessionStore<K, V> {

    fun getOrNull(sessionId: K): V?

    fun put(sessionId: K, value: V)

    fun remove(sessionId: K)
}