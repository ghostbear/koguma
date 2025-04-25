package me.ghostbear.koguma.domain.mediaQuery

sealed interface MediaResult {
    val mediaQuery: MediaQuery

    data class Success(val media: Media, override val mediaQuery: MediaQuery) : MediaResult
    data class NotFound(override val mediaQuery: MediaQuery) : MediaResult
    data class Failure(val message: String, override val mediaQuery: MediaQuery) : MediaResult

}
