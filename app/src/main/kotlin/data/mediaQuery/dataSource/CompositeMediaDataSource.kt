package me.ghostbear.koguma.data.mediaQuery.dataSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.model.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.model.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType

class CompositeMediaDataSource(
    dataSources: List<MediaDataSource>,
) : MediaDataSource {

    private val dataSources = dataSources
        .filterNot { it is CompositeMediaDataSource }

    override fun isSupported(type: MediaType): Boolean {
        return dataSources.any { it.isSupported(type) }
    }

    override suspend fun query(mediaQuery: MediaQuery): MediaResult {
        return dataSources
            .firstOrNull { it.isSupported(mediaQuery.type) }
            ?.query(mediaQuery)
            ?: MediaResult.Failure("Unsupported media type: ${mediaQuery.type}", mediaQuery)
    }

    override suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult>  = coroutineScope {
        withContext(Dispatchers.IO) {
            mediaQuery.map { async { query(it) } }.awaitAll()
        }
    }

}