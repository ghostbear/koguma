package me.ghostbear.koguma.data.mediaQuery

import com.example.generated.SearchMedia
import com.example.generated.enums.MediaFormat
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.CoroutineScope
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

class AniListMedia(
    val media: com.example.generated.searchmedia.Media
) : Media {

    override val id: Long
        get() = media.id.toLong()
    override val url: String
        get() = media.siteUrl ?: "https://anilist.co/${type.name.lowercase()}/${id}"
    override val title: String
        get() = media.title?.userPreferred ?: "Untitled"
    override val description: String
        get() = media.description ?: "No description"
    override val thumbnailUrl: String
        get() = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium ?: "https://placehold.co/500x700?text=?"
    override val type: MediaType
        get() = when (media.type ?: com.example.generated.enums.MediaType.__UNKNOWN_VALUE) {
            com.example.generated.enums.MediaType.ANIME -> MediaType.ANIME
            com.example.generated.enums.MediaType.MANGA -> if (media.format == MediaFormat.MANGA) MediaType.MANGA else MediaType.NOVEL
            com.example.generated.enums.MediaType.__UNKNOWN_VALUE -> throw IllegalArgumentException("Unknown media type")
        }
    override val year: Int
        get() = media.seasonYear ?: -1
    override val meanScore: Double
        get() = media.meanScore?.toDouble() ?: -1.0
    override val genres: List<String>
        get() = media.genres?.filterNotNull() ?: emptyList()

}