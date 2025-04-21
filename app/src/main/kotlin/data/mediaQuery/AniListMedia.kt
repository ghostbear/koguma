package me.ghostbear.koguma.data.mediaQuery

import kotlinx.datetime.LocalDate
import me.ghostbear.koguma.data.mediaQuery.aniList.SearchMediaQuery
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaFormat
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaType
import me.ghostbear.koguma.domain.mediaQuery.Media as DomainMedia
import me.ghostbear.koguma.domain.mediaQuery.MediaSeason as DomainMediaSeason
import me.ghostbear.koguma.domain.mediaQuery.MediaType as DomainMediaType
import me.ghostbear.koguma.domain.mediaQuery.MediaFormat as DomainMediaFormat
import me.ghostbear.koguma.domain.mediaQuery.MediaStatus as DomainMediaStatus

class AniListMedia(
    val media: SearchMediaQuery.Data.Page.Medium
) : DomainMedia {

    override val id: Long
        get() = media.id.toLong()
    override val url: String
        get() = media.siteUrl ?: "https://anilist.co/${type?.name?.lowercase()}/${id}"
    override val title: String
        get() = media.title?.userPreferred ?: "Untitled"
    override val description: String?
        get() = media.description?.replace("</?br>".toRegex(RegexOption.IGNORE_CASE), "")
            ?.replace("</?i>".toRegex(), "*")
    override val thumbnailUrl: String?
        get() = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium
    override val imageUrl: String?
        get() = media.bannerImage
    override val type: DomainMediaType?
        get() = when (media.type) {
            MediaType.ANIME -> DomainMediaType.ANIME
            MediaType.MANGA -> if (media.format == MediaFormat.MANGA) DomainMediaType.MANGA else DomainMediaType.NOVEL
            MediaType.UNKNOWN__, null -> null
        }
    override val format: DomainMediaFormat?
        get() = when (media.format) {
            MediaFormat.TV -> DomainMediaFormat.TV
            MediaFormat.TV_SHORT -> DomainMediaFormat.TV_SHORT
            MediaFormat.MOVIE -> DomainMediaFormat.MOVIE
            MediaFormat.SPECIAL -> DomainMediaFormat.SPECIAL
            MediaFormat.OVA -> DomainMediaFormat.OVA
            MediaFormat.ONA -> DomainMediaFormat.ONA
            MediaFormat.MUSIC -> DomainMediaFormat.MUSIC
            MediaFormat.MANGA -> DomainMediaFormat.MANGA
            MediaFormat.NOVEL -> DomainMediaFormat.NOVEL
            MediaFormat.ONE_SHOT -> DomainMediaFormat.ONE_SHOT
            MediaFormat.UNKNOWN__, null -> null
        }
    override val status: DomainMediaStatus?
        get() = when (media.status) {
            MediaStatus.FINISHED -> DomainMediaStatus.FINISHED
            MediaStatus.RELEASING -> DomainMediaStatus.RELEASING
            MediaStatus.NOT_YET_RELEASED -> DomainMediaStatus.NOT_YET_RELEASED
            MediaStatus.CANCELLED -> DomainMediaStatus.CANCELLED
            MediaStatus.HIATUS -> DomainMediaStatus.HIATUS
            MediaStatus.UNKNOWN__, null -> null
        }
    override val year: Int?
        get() = media.seasonYear
    override val season: DomainMediaSeason?
        get() = when(media.season) {
            MediaSeason.WINTER -> DomainMediaSeason.WINTER
            MediaSeason.SPRING -> DomainMediaSeason.SPRING
            MediaSeason.SUMMER -> DomainMediaSeason.SUMMER
            MediaSeason.FALL -> DomainMediaSeason.FALL
            MediaSeason.UNKNOWN__, null -> null
        }
    override val startDate: LocalDate?
        get() {
            val startDate = media.startDate ?: return null
            return LocalDate(
                startDate.year ?: return null,
                startDate.month ?: 1,
                startDate.day ?: 1,
            )
        }
    override val meanScore: Double?
        get() = media.meanScore?.toDouble()
    override val genres: List<String>?
        get() = media.genres?.filterNotNull()
    override val episodeCount: Int?
        get() = media.episodes
    override val chapterCount: Int?
        get() = media.chapters
    override val color: Int?
        get() = media.coverImage?.color?.substring(1)?.toInt(16)
}