package me.ghostbear.koguma.data.mediaQueryMatch

data class Token(
    val type: TokenType,
    val value: String? = null,
    val literal: Any? = null,
)