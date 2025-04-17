package me.ghostbear.koguma

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import me.ghostbear.koguma.data.mediaQuery.AniListMediaDataSource
import me.ghostbear.koguma.data.mediaQuery.CaffeineMediaQuerySessionizer
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.presentation.mediaQuery.ChannelIdAndMessageId
import me.ghostbear.koguma.presentation.mediaQuery.mediaQueryModule

suspend fun main(args: Array<String>) {
    val kord = Kord(args.firstOrNull() ?: error("Missing required argument 'token'"))

    kord.mediaQueryModule(
        AniListMediaDataSource(
            GraphQLKtorClient(
                url = URL("https://graphql.anilist.co/"),
                serializer = GraphQLClientKotlinxSerializer()
            )
        ),
        CaffeineMediaQuerySessionizer<ChannelIdAndMessageId>(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(32)
                .removalListener<ChannelIdAndMessageId, MediaQuery> { id, session, cause ->
                    if (cause == RemovalCause.REPLACED) return@removalListener
                    kord.launch {
                        val (channelId, messageId) = id!!
                        kord.rest.channel.editMessage(channelId, messageId) {
                            components = mutableListOf()
                        }
                    }
                }
                .build<ChannelIdAndMessageId, MediaQuery>()
        )
    )

    kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on
        if (message.content == "!ping") message.channel.createMessage("Pong!")
    }

    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}