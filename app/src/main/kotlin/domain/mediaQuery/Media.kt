package me.ghostbear.koguma.domain.mediaQuery

import kotlinx.datetime.LocalDate

interface Media {
    val id: Long
    val url: String
    val title: String
    val description: String?
    val thumbnailUrl: String?
    val type: MediaType?
    val format: MediaFormat?
    val status: MediaStatus?
    val year: Int?
    val season: MediaSeason?
    val startDate: LocalDate?
    val meanScore: Double?
    val genres: List<String>?
    val episodeCount: Int?
    val chapterCount: Int?
    val color: Int?
}

