package me.ghostbear.koguma

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.ktor.http.KtorHttpEngine
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.launch
import me.ghostbear.koguma.data.mediaQuery.AniListMediaDataSource
import me.ghostbear.koguma.data.mediaQueryParser.InterpreterMediaQueryMatcher
import me.ghostbear.koguma.data.session.CaffeineSessionStore
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.presentation.mediaQuery.MediaQuerySession
import me.ghostbear.koguma.presentation.mediaQuery.MediaQuerySessionId
import me.ghostbear.koguma.presentation.mediaQuery.mediaQueryModule

suspend fun main(args: Array<String>) {
    val kord = Kord(args.firstOrNull() ?: error("Missing required argument 'token'"))

    kord.mediaQueryModule(
        InterpreterMediaQueryMatcher(),
        AniListMediaDataSource(
            ApolloClient.Builder()
                .serverUrl("https://graphql.anilist.co/")
                .httpEngine(KtorHttpEngine())
                .normalizedCache(MemoryCacheFactory(50 * 1024 * 1024, 5.minutes.inWholeMilliseconds))
                .build()
        ),
        CaffeineSessionStore<MediaQuerySessionId, MediaQuerySession>(
            Caffeine.newBuilder()
                .expireAfterWrite(5.minutes.toJavaDuration())
                .maximumSize(32)
                .removalListener<MediaQuerySessionId, MediaQuerySession> { id, _, cause ->
                    if (cause == RemovalCause.REPLACED) return@removalListener
                    kord.launch {
                        val (channelId, messageId) = id!!
                        kord.rest.channel.editMessage(channelId, messageId) {
                            components = mutableListOf()
                        }
                    }
                }
                .build<MediaQuerySessionId, MediaQuerySession>()
        )
    )

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}