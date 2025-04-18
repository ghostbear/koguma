package me.ghostbear.koguma.data.mediaQuery

import com.example.generated.SearchMedia
import com.example.generated.enums.MediaFormat
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.ghostbear.koguma.domain.mediaQuery.Media
import me.ghostbear.koguma.domain.mediaQuery.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.MediaType

class AniListMediaDataSource(
    val client: GraphQLKtorClient,
) : MediaDataSource {
    override suspend fun query(mediaQuery: MediaQuery): MediaResult<Media> {
        val response = _query(mediaQuery)

        if (response.errors != null && response.errors!!.isNotEmpty()) {
            return MediaResult.Error.Message(response.errors!!.joinToString(separator = ",") { it.message }, mediaQuery)
        }

        val page = response.data?.Page
        if (page == null) {
            return MediaResult.Error.Message("Missing page", mediaQuery)
        }

        val pageInfo = page.pageInfo
        if (pageInfo == null) {
            return MediaResult.Error.Message("Missing page info", mediaQuery)
        }

        val mediaOrNull = page.media?.firstOrNull()
        if (mediaOrNull == null) {
            return MediaResult.Error.NotFound(mediaQuery)
        }

        return MediaResult.Success(
            AniListMedia(mediaOrNull),
            mediaQuery.copy(currentPage = pageInfo.currentPage ?: 0, lastPage = pageInfo.lastPage ?: 0)
        )

    }

    override suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult<Media>> = coroutineScope {
        withContext(Dispatchers.IO) {
            mediaQuery.map { async { query(it) } }.awaitAll()
        }
    }

    internal suspend fun _query(mediaQuery: MediaQuery): GraphQLClientResponse<SearchMedia.Result> {
        val formatIn = when (mediaQuery.type) {
            MediaType.ANIME -> null
            MediaType.MANGA -> listOf(MediaFormat.MANGA, MediaFormat.ONE_SHOT)
            MediaType.NOVEL -> listOf(MediaFormat.NOVEL)
        }

        val formatNotIn = when (mediaQuery.type) {
            MediaType.ANIME -> listOf(MediaFormat.MANGA, MediaFormat.ONE_SHOT, MediaFormat.NOVEL)
            MediaType.MANGA -> null
            MediaType.NOVEL -> null
        }

        val variables = SearchMedia.Variables(
            query = mediaQuery.query,
            page = mediaQuery.currentPage,
            format_in = formatIn,
            format_not_in = formatNotIn,
        )

        return client.execute(SearchMedia(variables))
    }

}

