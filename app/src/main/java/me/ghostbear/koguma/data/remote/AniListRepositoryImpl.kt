/*
 * Copyright (C) 2022 ghostbear
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package me.ghostbear.koguma.data.remote

import io.ktor.client.HttpClient
import java.io.IOException
import javax.inject.Inject
import me.ghostbear.koguma.data.remote.anilist.SearchForManga
import me.ghostbear.koguma.data.remote.anilist.SearchForMangaQuery
import me.ghostbear.koguma.data.remote.graphql.query
import me.ghostbear.koguma.data.mangaRemoteLocalMapper
import me.ghostbear.koguma.domain.model.Manga
import me.ghostbear.koguma.domain.repository.AniListRepository

class AniListRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
) : AniListRepository {

    override suspend fun search(query: String): List<Manga> {
        val response = httpClient.query<SearchForManga>(
            urlString = "https://graphql.anilist.co",
            query = SearchForMangaQuery(query)
        )

        val errors = response.errors

        if (errors != null) {
            throw IOException(
                buildString {
                    errors.forEachIndexed { index, error ->
                        append(error.message ?: return@forEachIndexed)
                        if (index != errors.lastIndex) append("\n")
                    }
                }
            )
        }

        if (response.data == null) {
            throw IllegalArgumentException("Response didn't include data or errors")
        }

        return response.data.page?.media
            ?.map(mangaRemoteLocalMapper) ?: emptyList()
    }

}