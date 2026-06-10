package me.ghostbear.koguma.core.mediaQueryMatch

import me.ghostbear.koguma.domain.mediaQuery.model.MediaType

interface MediaQueryMatch {
    val query: String
    val type: MediaType
    val page: Int
}