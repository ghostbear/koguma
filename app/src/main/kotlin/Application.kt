package me.ghostbear.koguma

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.SpanStatus
import me.ghostbear.koguma.di.KordContext
import me.ghostbear.koguma.di.KordModule
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.launch
import me.ghostbear.koguma.data.mediaQuery.CompositeMediaDataSource
import me.ghostbear.koguma.data.mediaQuery.aniList.cache.Cache
import me.ghostbear.koguma.data.mediaQueryAnilist.AniListMediaDataSource
import me.ghostbear.koguma.data.mediaQueryMangabaka.MangabakaMediaDataSource
import me.ghostbear.koguma.data.mediaQueryMatch.InterpreterMediaQueryMatcher
import me.ghostbear.koguma.ext.safely
import me.ghostbear.koguma.presentation.mediaQuery.DiscordMessageReference
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSession
import me.ghostbear.koguma.presentation.mediaQuery.mediaQueryModule
import me.ghostbear.koguma.core.session.CaffeineSessionStore

suspend fun main() {
    Sentry.init { options ->
        options.isEnabled = false
        options.isEnableExternalConfiguration = true
        options.isSendDefaultPii = true
        options.tracesSampleRate = 1.0
        options.environment = "development"
        options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
            if (!event.isErrored)
                event.removeExtra("content")
            event
        }
        options.beforeSendTransaction = SentryOptions.BeforeSendTransactionCallback { transaction, hint ->
            if (transaction.status != SpanStatus.UNKNOWN_ERROR)
                transaction.removeExtra("content")
            transaction
        }
    }

    startKoin {
        modules(applicationModule)
    }

    val koin = getKoin()
    val context = koin.get<KordContext>()
    val preference = koin.get<ApplicationPreference>()
    val kord = Kord(preference.token)
    context.kord = kord

    val modules = koin.getAll<KordModule>()
    modules.forEach { kordModule ->
        val module = kordModule.module
        module(kord)
    }

    kord.login {
        presence {
            watching("\uD83D\uDCFA the honey drip out of the jar")
        }
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}
