package me.ghostbear.koguma.domain.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType

interface MediaQueryMatch {
    val query: String
    val type: MediaType
    val page: Int
}