package me.ghostbear.koguma.data.mediaQueryMangabaka.dataSource.remote

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaResponse(
    val status: MangabakaStatus,
    val pagination: MangabakaPagigation,
    val data: List<MangabakaMedium>,
)

@Serializable(with = MangabakaStatus.Serializer::class)
enum class MangabakaStatus(private val code: Int) {
    OK(code = 200);

    companion object Serializer : EnumAsIntSerializer<MangabakaStatus>(
        "MangabakaStatus",
        { e -> e.code },
        { v -> entries.first { e -> e.code == v } }
    )
}

open class EnumAsIntSerializer<T : Enum<*>>(
    serialName: String,
    val serialize: (v: T) -> Int,
    val deserialize: (v: Int) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(serialize(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val v = decoder.decodeInt()
        return deserialize(v)
    }
}

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaPagigation(
    val count: UInt,
    val page: UInt,
    val limit: UInt,
    val next: String?,
    val previous: String?,
)

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMedium(
    val id: ULong,
    val type: MangabakaMediaType,
    val titles: List<MangabakaMediaTitle>?,
    val description: String?,
    val rating: Double?,
    val cover: MangabakaMediaCover?,
    @SerialName("genres_v2")
    val genresV2: List<MangabakaMediaGenre>?,
    val genres: List<String>?,
    @SerialName("total_chapters")
    val totalChapters: String?,
    val source: MangabakaMediaSource,
    val status: MangabakaMediaStatus,
    val published: MangabakaMediaPublished,
)

@Serializable
enum class MangabakaMediaType {
    @SerialName("manga")
    MANGA,
    @SerialName("novel")
    NOVEL,
    @SerialName("manwha")
    MANWHA,
    @SerialName("manhua")
    MANHUA,
    @SerialName("oel")
    OEL,
    @SerialName("other")
    OTHER,
}

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMediaTitle(
    val language: String,
    val traits: List<MangabakaMediaTitleTrait>,
    val title: String,
    @SerialName("is_primary")
    val isPrimary: Boolean?,
)

@Serializable
enum class MangabakaMediaTitleTrait {
    @SerialName("official")
    OFFICIAL,
    @SerialName("native")
    NATIVE,
    @SerialName("alternative")
    ALTERNATIVE,
}

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMediaCover(
    val raw: Raw,
    @SerialName("x150")
    val small: Scaled,
    @SerialName("x250")
    val medium: Scaled,
    @SerialName("x350")
    val large: Scaled,
) {

    @Serializable
    @JsonIgnoreUnknownKeys
    data class Raw(
        val url: String?
    )

    @Serializable
    data class Scaled(
        @SerialName("x1")
        val small: String?,
        @SerialName("x2")
        val medium: String?,
        @SerialName("x3")
        val large: String?,
    )
}

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMediaGenre(
    val name: String
)

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMediaSource(
    val anilist: MangaBakaMediaSourceProvider<UInt>,
    @SerialName("anime_planet")
    val animePlanet: MangaBakaMediaSourceProvider<String>,
    @SerialName("anime_news_network")
    val animeNewsNetwork: MangaBakaMediaSourceProvider<UInt>,
    val kitsu: MangaBakaMediaSourceProvider<UInt>,
    @SerialName("manga_updates")
    val mangaUpdates: MangaBakaMediaSourceProvider<String>,
    @SerialName("my_anime_list")
    val myAnimeList: MangaBakaMediaSourceProvider<UInt>,
    val shikimori: MangaBakaMediaSourceProvider<UInt>
) {

    @Serializable
    @JsonIgnoreUnknownKeys
    data class MangaBakaMediaSourceProvider<T>(
        val id: T?
    )
}

@Serializable
enum class MangabakaMediaStatus {
    @SerialName("cancelled")
    CANCELLED,
    @SerialName("completed")
    COMPLETED,
    @SerialName("hiatus")
    HIATUS,
    @SerialName("releasing")
    RELEASING,
    @SerialName("unknown")
    UNKNOWN,
    @SerialName("upcoming")
    UPCOMING,
}

@Serializable
@JsonIgnoreUnknownKeys
data class MangabakaMediaPublished(
    @SerialName("start_date")
    val startDate: LocalDate?,
    @SerialName("end_date")
    val endDate: LocalDate?,
)
