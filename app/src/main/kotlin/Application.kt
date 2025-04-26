package me.ghostbear.koguma

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.ktor.http.KtorHttpEngine
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.typesafe.config.ConfigFactory
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.SpanStatus
import io.sentry.protocol.SentryTransaction
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.launch
import me.ghostbear.koguma.data.mediaQuery.AniListMediaDataSource
import me.ghostbear.koguma.data.mediaQueryParser.InterpreterMediaQueryMatcher
import me.ghostbear.koguma.ext.safely
import me.ghostbear.koguma.presentation.mediaQuery.DiscordMessageReference
import me.ghostbear.koguma.presentation.mediaQuery.DiscordSession
import me.ghostbear.koguma.presentation.mediaQuery.mediaQueryModule
import me.ghostbear.koguma.session.CaffeineSessionStore

suspend fun main() {
    val root = ConfigFactory.load()

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

    val config = root.safely { getConfig("koguma") } ?: throw IllegalStateException("Missing configuration '$.koguma'")
    val token: String = config.safely { getString("token") } ?: error("Missing required configuration '$.koguma.token'")

    val kord = Kord(token)
    kord.mediaQueryModule(
        InterpreterMediaQueryMatcher(),
        AniListMediaDataSource(
            ApolloClient.Builder()
                .serverUrl("https://graphql.anilist.co/")
                .httpEngine(
                    KtorHttpEngine(
                        HttpClient {
                            expectSuccess = false
                            install(HttpTimeout) {
                                connectTimeoutMillis = 1.minutes.inWholeMilliseconds
                                requestTimeoutMillis = 1.minutes.inWholeMilliseconds
                            }
                        }
                    )
                )
                .normalizedCache(MemoryCacheFactory(50 * 1024 * 1024, 5.minutes.inWholeMilliseconds))
                .build()
        ),
        CaffeineSessionStore<DiscordMessageReference, DiscordSession>(
            Caffeine.newBuilder()
                .expireAfterWrite(5.minutes.toJavaDuration())
                .maximumSize(64)
                .removalListener<DiscordMessageReference, DiscordSession> { id, session, cause ->
                    if (cause == RemovalCause.REPLACED) return@removalListener
                    kord.launch {
                        when {
                            session is DiscordSession.Message -> {
                                val (messageId, channelId) = session.replyReference
                                kord.rest.channel.editMessage(channelId, messageId) {
                                    components = mutableListOf()
                                }
                            }

                            else -> {
                                val (messageId, channelId) = id ?: return@launch
                                kord.rest.channel.editMessage(channelId, messageId) {
                                    components = mutableListOf()
                                }
                            }
                        }
                    }
                }
                .build<DiscordMessageReference, DiscordSession>()
        )
    )

    kord.login {
        presence {
            watching("anime")
        }
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}