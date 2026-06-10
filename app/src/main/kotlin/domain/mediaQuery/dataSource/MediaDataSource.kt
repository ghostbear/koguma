package me.ghostbear.koguma.domain.mediaQuery.dataSource

import me.ghostbear.koguma.domain.mediaQuery.model.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.model.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType

interface MediaDataSource {

    fun isSupported(type: MediaType): Boolean

    suspend fun query(mediaQuery: MediaQuery): MediaResult

    suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult>

}