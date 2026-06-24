package me.ghostbear.koguma.session

import com.github.benmanes.caffeine.cache.Caffeine
import me.ghostbear.koguma.core.session.CaffeineSessionStore
import me.ghostbear.koguma.core.session.SessionStore
import me.ghostbear.koguma.presentation.mediaQuery.DiscordMessageReference
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSession
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSessionRemovalListener
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

val sessionModule = module {
    single {
        Caffeine.newBuilder()
            .expireAfterWrite(5.minutes.toJavaDuration())
            .maximumSize(64)
            .removalListener(get<DiscordSessionRemovalListener>())
            .build<DiscordMessageReference, DiscordSession>()
    }

    single<SessionStore<DiscordMessageReference, DiscordSession>> {
        CaffeineSessionStore(get())
    }
}
