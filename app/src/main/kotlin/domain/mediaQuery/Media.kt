package me.ghostbear.koguma.domain.mediaQuery

import kotlinx.datetime.DateTimeUnit

interface Media {
    val id: Long
    val title: String
    val description: String
    val thumbnailUrl: String
    val type: MediaType
    val year: Int
    val meanScore: Double
    val genres: List<String>
}

