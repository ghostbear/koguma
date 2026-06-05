package me.ghostbear.koguma.data.mediaQueryMangabaka

import kotlinx.datetime.LocalDate
import me.ghostbear.koguma.domain.mediaQuery.MediaFormat
import me.ghostbear.koguma.domain.mediaQuery.MediaSeason
import me.ghostbear.koguma.domain.mediaQuery.MediaStatus
import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQuery.Media as DomainMedia

class MangabakaMedia(
    private val value: MangabakaMedium
) : DomainMedia {
    override val id: Long
        get() = value.id.toLong()
    override val url: String
        get() = "https://mangabaka.org/${id}"
    override val title: String
        get() = value.titles
            ?.filter {
                it.language == "en"
                        || it.language == "ja"
                        || it.language == "jp-Latn"
                        || it.language == "ko"
                        || it.language == "ko-Latn"
                        || it.language == "zh"
                        || it.language == "zh-hk"
                        || it.language == "zh-Latn"
            }
            ?.find { it.isPrimary == true }
            ?.title ?: "Unknown title"
    override val description: String?
        get() = value.description
    override val thumbnailUrl: String?
        get() = value.cover?.raw?.url
            ?: value.cover?.large?.large
            ?: value.cover?.large?.medium
            ?: value.cover?.large?.small
            ?: value.cover?.medium?.large
            ?: value.cover?.medium?.medium
            ?: value.cover?.medium?.small
            ?: value.cover?.small?.large
            ?: value.cover?.small?.medium
            ?: value.cover?.small?.small
    override val imageUrl: String? = null
    override val type: MediaType? = MediaType.MANGA
    override val format: MediaFormat?
        get() = when (value.type) {
            MangabakaMediaType.MANGA -> MediaFormat.MANGA
            MangabakaMediaType.MANWHA -> MediaFormat.MANGA
            MangabakaMediaType.MANHUA -> MediaFormat.MANGA
            MangabakaMediaType.OEL -> MediaFormat.MANGA
            MangabakaMediaType.NOVEL -> MediaFormat.NOVEL
            MangabakaMediaType.OTHER -> MediaFormat.SPECIAL
        }
    override val status: MediaStatus?
        get() = when (value.status) {
            MangabakaMediaStatus.CANCELLED -> MediaStatus.CANCELLED
            MangabakaMediaStatus.COMPLETED -> MediaStatus.FINISHED
            MangabakaMediaStatus.HIATUS -> MediaStatus.HIATUS
            MangabakaMediaStatus.RELEASING -> MediaStatus.RELEASING
            MangabakaMediaStatus.UPCOMING -> MediaStatus.NOT_YET_RELEASED
            MangabakaMediaStatus.UNKNOWN -> null
        }
    override val year: Int?
        get() = value.published.startDate?.year
    override val season: MediaSeason? = null
    override val startDate: LocalDate?
        get() = value.published.startDate
    override val meanScore: Double? = null
    override val genres: List<String>?
        get() = value.genres?.map { it.name }
    override val episodeCount: Int? = null
    override val chapterCount: Int?
        get() = value.totalChapters?.toIntOrNull()
    override val color: Int?
        get() = when (value.type) {
            MangabakaMediaType.MANGA -> 0xFF0000
            MangabakaMediaType.NOVEL -> 0xFF0000
            MangabakaMediaType.MANWHA -> 0xFF0000
            MangabakaMediaType.MANHUA -> 0xFF0000
            MangabakaMediaType.OEL -> 0xFF0000
            MangabakaMediaType.OTHER -> 0xFF0000
        }
}