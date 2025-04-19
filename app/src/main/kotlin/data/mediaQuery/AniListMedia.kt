package me.ghostbear.koguma.data.mediaQuery

import me.ghostbear.koguma.data.mediaQuery.aniList.SearchMediaQuery
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaFormat
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaType
import me.ghostbear.koguma.domain.mediaQuery.Media as DomainMedia
import me.ghostbear.koguma.domain.mediaQuery.MediaType as DomainMediaType

class AniListMedia(
    val media: SearchMediaQuery.Data.Page.Medium
) : DomainMedia {

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
    override val type: DomainMediaType
        get() = when (media.type ?: MediaType.UNKNOWN__) {
            MediaType.ANIME -> DomainMediaType.ANIME
            MediaType.MANGA -> if (media.format == MediaFormat.MANGA) DomainMediaType.MANGA else DomainMediaType.NOVEL
            MediaType.UNKNOWN__ -> throw IllegalArgumentException("Unknown media type")
        }
    override val year: Int
        get() = media.seasonYear ?: -1
    override val meanScore: Double
        get() = media.meanScore?.toDouble() ?: -1.0
    override val genres: List<String>
        get() = media.genres?.filterNotNull() ?: emptyList()

}