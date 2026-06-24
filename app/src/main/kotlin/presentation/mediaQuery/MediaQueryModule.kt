package me.ghostbear.koguma.presentation.mediaQuery

import com.github.benmanes.caffeine.cache.Caffeine
import me.ghostbear.koguma.core.session.CaffeineSessionStore
import me.ghostbear.koguma.core.session.SessionStore
import me.ghostbear.koguma.di.KordModule
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

val MediaQueryCaffeine = named("mediaQueryCaffeine")
val MediaQuerySessionStore = named("mediaQuerySessionStore")

val mediaQueryPresentationModule = module {
    single(MediaQueryCaffeine) {
        Caffeine.newBuilder()
            .expireAfterWrite(5.minutes.toJavaDuration())
            .maximumSize(64)
            .removalListener(get<DiscordSessionRemovalListener>())
            .build<DiscordMessageReference, DiscordSession>()
    }

    single<SessionStore<DiscordMessageReference, DiscordSession>>(MediaQuerySessionStore) {
        CaffeineSessionStore(get(MediaQueryCaffeine))
    }


    singleOf(::DiscordSessionRemovalListener)

    single<KordModule> {
        MediaQueryKordModule(get(), get(), get(MediaQuerySessionStore))
    }

}
