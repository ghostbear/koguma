package me.ghostbear.koguma.core.mediaQueryMatch.mediaQueryMatch

data class Token(
    val type: TokenType,
    val value: String? = null,
    val literal: Any? = null,
)