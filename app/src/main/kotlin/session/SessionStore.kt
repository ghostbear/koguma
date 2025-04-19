package me.ghostbear.koguma.session

interface SessionStore<K, V> {

    fun getOrNull(sessionId: K): V?

    fun put(sessionId: K, value: V)

}