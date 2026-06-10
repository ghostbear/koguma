package me.ghostbear.koguma.domain.mediaQuery

data class MediaLink(
    val id: Id,
    val url: String,
    val isPrimary: Boolean = false
) {
    init {
        require(url.isNotBlank()) { "URL cannot be blank" }
    }

    sealed interface Id {
        val id: String

        data object AniList : Id {
            override val id: String = "anilist"
        }

        data object MyAnimeList : Id {
            override val id: String = "myanimelist"
        }

        data object Kitsu : Id {
            override val id: String = "kitsu"
        }

        data object MangaUpdates : Id {
            override val id: String = "mangaupdates"
        }

        data object MangaBaka : Id {
            override val id: String = "mangabaka"
        }

        data object AnimePlanet : Id {
            override val id: String = "animeplanet"
        }

        data object AnimeNewsNetwork : Id {
            override val id: String = "animenewsnetwork"
        }

        data object Shikimori : Id {
            override val id: String = "shikimori"
        }

        data class Unknown(override val id: String) : Id

        companion object {
            val values = listOf<Id>(
                AniList,
                MyAnimeList,
                Kitsu,
                MangaUpdates,
                AnimePlanet,
                AnimeNewsNetwork,
                Shikimori
            )

            fun fromString(id: String): Id = values.firstOrNull { it.id == id } ?: Unknown(id)
        }
    }
}
