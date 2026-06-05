package me.ghostbear.koguma

import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache
import com.apollographql.ktor.http.KtorHttpEngine
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.typesafe.config.ConfigFactory
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.SpanStatus
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
        CompositeMediaDataSource(
            listOf(
                MangabakaMediaDataSource(HttpClient {
                    expectSuccess = false
                    install(ContentNegotiation) {
                        json()
                    }
                }),
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
                        .normalizedCache(
                            normalizedCacheFactory = MemoryCacheFactory(50 * 1024 * 1024, 5.minutes.inWholeMilliseconds),
                            cacheKeyGenerator = TypePolicyCacheKeyGenerator(
                                typePolicies = Cache.typePolicies,
                                keyScope = CacheKey.Scope.SERVICE
                            ),
                            cacheResolver = FieldPolicyCacheResolver(
                                keyScope = CacheKey.Scope.SERVICE,
                                fieldPolicies = Cache.fieldPolicies
                            )
                        )
                        .build()
                )
            )
        ),
        CaffeineSessionStore(
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
                .build()
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