package me.ghostbear.koguma.data.mediaQueryAnilist.dataSource

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.*
import me.ghostbear.koguma.data.mediaQuery.aniList.SearchMediaQuery
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaFormat
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.model.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.model.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType
import me.ghostbear.koguma.ext.trace

class AniListMediaDataSource(
    val apolloClient: ApolloClient,
) : MediaDataSource {

    override fun isSupported(type: MediaType): Boolean {
        return true
    }

    override suspend fun query(mediaQuery: MediaQuery): MediaResult = trace("anilist", "query") {
        val response = internalQuery(mediaQuery)

        if (response.errors != null && response.errors!!.isNotEmpty()) {
            return MediaResult.Failure(response.errors!!.joinToString(separator = ",") { it.message }, mediaQuery)
        }

        val page = response.data?.page ?: return MediaResult.Failure("Missing page", mediaQuery)

        val pageInfo = page.pageInfo ?: return MediaResult.Failure("Missing page info", mediaQuery)

        val mediaOrNull = page.media?.firstOrNull() ?: return MediaResult.NotFound(mediaQuery)

        return MediaResult.Success(
            AniListMedia(mediaOrNull),
            mediaQuery.copy(currentPage = pageInfo.currentPage ?: 0, lastPage = pageInfo.lastPage ?: 0)
        )

    }

    override suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult> = coroutineScope {
        withContext(Dispatchers.IO) {
            mediaQuery.map { async { query(it) } }.awaitAll()
        }
    }

    internal suspend fun internalQuery(mediaQuery: MediaQuery): ApolloResponse<SearchMediaQuery.Data> {
        val formatIn: List<MediaFormat>? = when (mediaQuery.type) {
            MediaType.ANIME -> null
            MediaType.MANGA -> listOf(MediaFormat.MANGA, MediaFormat.ONE_SHOT)
            MediaType.NOVEL -> listOf(MediaFormat.NOVEL)
        }

        val formatNotIn: List<MediaFormat>? = when (mediaQuery.type) {
            MediaType.ANIME -> listOf(MediaFormat.MANGA, MediaFormat.ONE_SHOT, MediaFormat.NOVEL)
            MediaType.MANGA -> null
            MediaType.NOVEL -> null
        }

        val query = SearchMediaQuery(
            query = mediaQuery.query,
            page = mediaQuery.currentPage,
            isAdult = mediaQuery.isNsfwChannel,
            format_in = Optional.presentIfNotNull(formatIn),
            format_not_in = Optional.presentIfNotNull(formatNotIn),
        )

        return apolloClient.query(query).execute()
    }

}