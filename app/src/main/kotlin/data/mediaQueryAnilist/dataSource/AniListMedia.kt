package me.ghostbear.koguma.data.mediaQueryAnilist.dataSource

import kotlinx.datetime.LocalDate
import me.ghostbear.koguma.data.mediaQuery.aniList.SearchMediaQuery
import me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaFormat
import me.ghostbear.koguma.domain.mediaQuery.model.Media
import me.ghostbear.koguma.domain.mediaQuery.model.MediaLink
import me.ghostbear.koguma.domain.mediaQuery.model.MediaSeason
import me.ghostbear.koguma.domain.mediaQuery.model.MediaStatus
import me.ghostbear.koguma.domain.mediaQuery.model.MediaType

class AniListMedia(
    val media: SearchMediaQuery.Data.Page.Medium
) : Media {

    override val id: Long
        get() = media.id.toLong()
    override val links: List<MediaLink>
        get() = buildList {
            add(
                MediaLink(
                    MediaLink.Id.AniList,
                    media.siteUrl ?: "https://anilist.co/${type?.name?.lowercase()}/${id}",
                    true
                )
            )
        }
    override val title: String
        get() = media.title?.userPreferred ?: "Untitled"
    override val description: String?
        get() = media.description?.replace("</?br>".toRegex(RegexOption.IGNORE_CASE), "")
            ?.replace("</?i>".toRegex(), "*")
    override val thumbnailUrl: String?
        get() = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium
    override val imageUrl: String?
        get() = media.bannerImage
    override val type: MediaType?
        get() = when (media.type) {
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaType.ANIME -> MediaType.ANIME
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaType.MANGA -> if (media.format == MediaFormat.MANGA) MediaType.MANGA else MediaType.NOVEL
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaType.UNKNOWN__, null -> null
        }
    override val format: me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat?
        get() = when (media.format) {
            MediaFormat.TV -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.TV
            MediaFormat.TV_SHORT -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.TV_SHORT
            MediaFormat.MOVIE -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.MOVIE
            MediaFormat.SPECIAL -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.SPECIAL
            MediaFormat.OVA -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.OVA
            MediaFormat.ONA -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.ONA
            MediaFormat.MUSIC -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.MUSIC
            MediaFormat.MANGA -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.MANGA
            MediaFormat.NOVEL -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.NOVEL
            MediaFormat.ONE_SHOT -> me.ghostbear.koguma.domain.mediaQuery.model.MediaFormat.ONE_SHOT
            MediaFormat.UNKNOWN__, null -> null
        }
    override val status: MediaStatus?
        get() = when (media.status) {
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.FINISHED -> MediaStatus.FINISHED
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.RELEASING -> MediaStatus.RELEASING
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.NOT_YET_RELEASED -> MediaStatus.NOT_YET_RELEASED
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.CANCELLED -> MediaStatus.CANCELLED
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.HIATUS -> MediaStatus.HIATUS
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaStatus.UNKNOWN__, null -> null
        }
    override val year: Int?
        get() = media.seasonYear
    override val season: MediaSeason?
        get() = when(media.season) {
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason.WINTER -> MediaSeason.WINTER
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason.SPRING -> MediaSeason.SPRING
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason.SUMMER -> MediaSeason.SUMMER
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason.FALL -> MediaSeason.FALL
            me.ghostbear.koguma.data.mediaQuery.aniList.type.MediaSeason.UNKNOWN__, null -> null
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