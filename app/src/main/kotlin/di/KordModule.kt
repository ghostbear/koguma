package me.ghostbear.koguma.di

import dev.kord.core.Kord

interface KordModule {

    val module: suspend Kord.() -> Unit

}