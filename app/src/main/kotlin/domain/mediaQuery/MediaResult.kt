package me.ghostbear.koguma.domain.mediaQuery

sealed interface MediaResult<out T : Media> {
    val mediaQuery: MediaQuery

    data class Success(val media: Media, override val mediaQuery: MediaQuery) : MediaResult<Media>

    sealed interface Error<out T : Media> : MediaResult<T> {
        data class NotFound(override val mediaQuery: MediaQuery) : MediaResult<Nothing>
        data class Message(val message: String, override val mediaQuery: MediaQuery) : MediaResult<Nothing>
    }

}
