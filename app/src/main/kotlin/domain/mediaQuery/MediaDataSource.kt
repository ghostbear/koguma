package me.ghostbear.koguma.domain.mediaQuery

interface MediaDataSource {

    fun isSupported(type: MediaType): Boolean

    suspend fun query(mediaQuery: MediaQuery): MediaResult

    suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult>

}