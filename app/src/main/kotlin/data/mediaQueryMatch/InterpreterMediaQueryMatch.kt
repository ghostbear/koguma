package me.ghostbear.koguma.data.mediaQueryParser

import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatch

data class InterpreterMediaQueryMatch(
    override val query: String,
    override val type: MediaType
) : MediaQueryMatch