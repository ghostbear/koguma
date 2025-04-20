package me.ghostbear.koguma.domain.mediaQuery

data class MediaQuery(
    val query: String,
    val type: MediaType,
    val allowNsfw: Boolean = false,
    val currentPage: Int = 1,
    val lastPage: Int = -1
)
