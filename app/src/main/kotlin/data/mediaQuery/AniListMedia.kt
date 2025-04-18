package me.ghostbear.koguma.data.mediaQuery

import com.example.generated.enums.MediaFormat
import com.example.generated.searchmedia.Media as DataMedia
import me.ghostbear.koguma.domain.mediaQuery.Media as DomainMedia
import me.ghostbear.koguma.domain.mediaQuery.MediaType

class AniListMedia(
    val media: DataMedia
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