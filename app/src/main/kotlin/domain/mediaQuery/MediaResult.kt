package me.ghostbear.koguma.domain.mediaQuery

sealed interface MediaResult<out T : Media> {
    data class Success<T : Media>(val media: T, val mediaQuery: MediaQuery) : MediaResult<T>
    data class Error(val message: String) : MediaResult<Nothing>
}
