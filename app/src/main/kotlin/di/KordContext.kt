package me.ghostbear.koguma.di

import dev.kord.core.Kord
import dev.kord.rest.service.RestClient

class KordContext {
    lateinit var kord: Kord

    val rest: RestClient
        get() = kord.rest
}