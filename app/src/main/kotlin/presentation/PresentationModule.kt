package me.ghostbear.koguma.presentation

import me.ghostbear.koguma.presentation.mediaQuery.mediaQueryPresentationModule
import org.koin.dsl.module

val presentationModule = module {
    includes(mediaQueryPresentationModule)
}
