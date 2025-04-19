package me.ghostbear.koguma.ext

inline fun <reified R> Any?.takeIf(): R? {
    if (this is R) return this
    return null
}