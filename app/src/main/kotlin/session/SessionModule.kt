package me.ghostbear.koguma.session

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import dev.kord.core.Kord
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.launch
import me.ghostbear.koguma.presentation.mediaQuery.DiscordMessageReference
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSession
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSessionRemovalListener
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

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
