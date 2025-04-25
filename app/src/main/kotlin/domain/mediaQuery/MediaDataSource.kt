package me.ghostbear.koguma.domain.mediaQuery

interface MediaDataSource {

    suspend fun query(mediaQuery: MediaQuery): MediaResult

    suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult>

}