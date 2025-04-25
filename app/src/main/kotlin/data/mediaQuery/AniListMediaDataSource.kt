package me.ghostbear.koguma.data.mediaQuery

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.ghostbear.koguma.data.mediaQuery.aniList.SearchMediaQuery
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaFormat
import me.ghostbear.koguma.domain.mediaQuery.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.MediaType

class AniListMediaDataSource(
    val apolloClient: ApolloClient,
) : MediaDataSource {
    override suspend fun query(mediaQuery: MediaQuery): MediaResult {
        val response = _query(mediaQuery)

        if (response.errors != null && response.errors!!.isNotEmpty()) {
            return MediaResult.Failure(response.errors!!.joinToString(separator = ",") { it.message }, mediaQuery)
        }

        val page = response.data?.page
        if (page == null) {
            return MediaResult.Failure("Missing page", mediaQuery)
        }

        val pageInfo = page.pageInfo
        if (pageInfo == null) {
            return MediaResult.Failure("Missing page info", mediaQuery)
        }

        val mediaOrNull = page.media?.firstOrNull()
        if (mediaOrNull == null) {
            return MediaResult.NotFound(mediaQuery)
        }

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

    internal suspend fun _query(mediaQuery: MediaQuery): ApolloResponse<SearchMediaQuery.Data> {
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

        val genresNotIn = listOf("Hentai", "Ecchi").takeUnless { mediaQuery.allowNsfw }

        val query = SearchMediaQuery(
            query = mediaQuery.query,
            page = mediaQuery.currentPage,
            format_in = Optional.presentIfNotNull(formatIn),
            format_not_in = Optional.presentIfNotNull(formatNotIn),
            genres_not_in = Optional.presentIfNotNull(genresNotIn)
        )

        return apolloClient.query(query).execute()
    }

}

