package me.ghostbear.koguma.domain.mediaQuery

interface MediaDataSource {

    suspend fun query(mediaQuery: MediaQuery): MediaResult<Media>

    suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult<Media>>

}