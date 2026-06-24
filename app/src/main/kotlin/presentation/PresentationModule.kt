package me.ghostbear.koguma.presentation

import me.ghostbear.koguma.di.KordModule
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSessionRemovalListener
import me.ghostbear.koguma.presentation.mediaQuery.MediaQueryKordModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    singleOf(::DiscordSessionRemovalListener)
    singleOf(::MediaQueryKordModule) bind KordModule::class
}
