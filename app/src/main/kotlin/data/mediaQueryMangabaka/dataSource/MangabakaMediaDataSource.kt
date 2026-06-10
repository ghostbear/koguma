package me.ghostbear.koguma.data.mediaQueryMangabaka.dataSource

import dev.kord.rest.request.isError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import me.ghostbear.koguma.data.mediaQueryMangabaka.dataSource.remote.MangabakaResponse
import me.ghostbear.koguma.domain.mediaQuery.dataSource.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.model.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.model.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType

class MangabakaMediaDataSource(private val httpClient: HttpClient) : MediaDataSource {

    override fun isSupported(type: MediaType): Boolean {
        return type in SuppoertedMediaTypes
    }

    override suspend fun query(mediaQuery: MediaQuery): MediaResult {
        return internalQuery(mediaQuery)
    }

    override suspend fun query(vararg mediaQuery: MediaQuery): List<MediaResult>  = coroutineScope {
        withContext(Dispatchers.IO) {
            mediaQuery.map { async { query(it) } }.awaitAll()
        }
    }

    internal suspend fun internalQuery(mediaQuery: MediaQuery): MediaResult {
        require(mediaQuery.type in SuppoertedMediaTypes) { "MangaBaka only supports manga and novel" }
        val response = try {
            httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.mangabaka.org"
                    encodedPath = "/v1/series/search"
                    parameter("q", mediaQuery.query)
                    parameter("page", mediaQuery.currentPage)
                    parameter("limit", "1")
                    when (mediaQuery.type) {
                        MediaType.MANGA -> {
                            parameter("type", "manga")
                            parameter("type", "manhwa")
                            parameter("type", "manhua")
                            parameter("type", "oel")
                            parameter("type", "other")
                        }

                        MediaType.NOVEL -> {
                            parameter("type", "novel")
                            parameter("type", "other")
                        }

                        MediaType.ANIME -> {}
                    }
                    if (!mediaQuery.isNsfwChannel) {
                        parameter("content_rating", "safe")
                        parameter("content_rating", "suggestive")
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }

        if (response.isError) {
            return MediaResult.Failure(response.body<JsonObject>().toString(), mediaQuery)
        }

        val body = response.body<MangabakaResponse>()

        val first = body.data.firstOrNull() ?: return MediaResult.NotFound(mediaQuery)

        return MediaResult.Success(
            media = MangabakaMedia(first),
            mediaQuery = mediaQuery.copy(
                currentPage = body.pagination.page.toInt(),
                lastPage = body.pagination.count.toInt()
            )
        )
    }

    companion object {
        val SuppoertedMediaTypes = listOf(MediaType.MANGA, MediaType.NOVEL)
    }
}