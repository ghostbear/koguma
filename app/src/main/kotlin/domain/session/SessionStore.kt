package me.ghostbear.koguma.domain.session

interface SessionStore<K, V> {

    fun getOrNull(sessionId: K): V?

    fun put(sessionId: K, value: V)

}