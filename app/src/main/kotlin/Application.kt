package me.ghostbear.koguma

import com.example.generated.SearchMedia
import com.example.generated.enums.MediaFormat
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.eygraber.uri.Uri
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

data class MediaSessionId(
    val channelId: Snowflake,
    val messageId: Snowflake,
)

fun Message.toSessionId(): MediaSessionId {
    return MediaSessionId(
        channelId = channelId,
        messageId = id
    )
}

data class MediaQuerySession(
    val query: String,
    val page: Int = 1,
)

fun SearchMedia.Result.toResponse(): MessageBuilder.() -> Unit = {
    embed {
        val media = Page?.media?.first()
        title = media?.title?.userPreferred
        description = media?.description
        thumbnail {
            url = media?.coverImage?.extraLarge ?: media?.coverImage?.large ?: media?.coverImage?.medium!!
        }
        field {
            name = "Type"
            value = media?.type?.name!!
            inline = true
        }
        field {
            name = "Year"
            value = "${media?.seasonYear}"
            inline = true
        }
        field {
            name = "Mean Score"
            value = "${media?.meanScore}"
            inline = true
        }
        field {
            name = "Genres"
            value = media?.genres?.joinToString(", ")!!
        }
    }
    actionRow {
        val pageInfo = Page?.pageInfo
        interactionButton(ButtonStyle.Primary, "koguma://previous") {
            disabled = pageInfo?.currentPage!! <= 1
            label = "Previous"
        }
        interactionButton(ButtonStyle.Secondary, "koguma://nothing") {
            disabled = true
            label = "${pageInfo?.currentPage}/${pageInfo?.lastPage}"
        }
        interactionButton(ButtonStyle.Primary, "koguma://next") {
            disabled = pageInfo?.currentPage!! >= pageInfo.lastPage!!
            label = "Next"
        }
    }
}

class AniListMediaDataSource(
    val httpClient: GraphQLKtorClient,
) {
    suspend fun query(query: String, page: Int = 1): SearchMedia.Result? {
        return httpClient.execute(
            SearchMedia(
                variables = SearchMedia.Variables(
                    query = query,
                    page = page,
                    format_not_in = listOf(MediaFormat.MANGA, MediaFormat.NOVEL)
                )
            )
        ).data
    }
}

val logger = LoggerFactory.getLogger("me.ghostbear.koguma.ApplicationKt")

suspend fun main(args: Array<String>) {
    val kord = Kord(args.firstOrNull() ?: error("Missing required argument 'token'"))

    val session: Cache<MediaSessionId, MediaQuerySession> = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(32)
        .removalListener<MediaSessionId, MediaQuerySession> { id, session, cause ->
            if (cause == RemovalCause.REPLACED) return@removalListener
            logger.info("Removed session $id")
            kord.launch {
                val (channelId, messageId) = id!!
                kord.rest.channel.editMessage(channelId, messageId) {
                    components = mutableListOf()
                }
            }
        }
        .build()

    val client = GraphQLKtorClient(
        url = URL("https://graphql.anilist.co/"),
        serializer = GraphQLClientKotlinxSerializer()
    )
    val aniListMediaDataSource = AniListMediaDataSource(client)

    kord.createGlobalChatInputCommand(
        "anime",
        "Search for an anime",
    ) {
        string("query", "Search query") {
            required = true
        }
    }

    kord.on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        if (command.rootName == "anime") {
            val deferredResponse = interaction.deferPublicResponse()
            val query = command.strings["query"]!!

            val media = aniListMediaDataSource.query(query)

            val response = deferredResponse.respond {
                media?.toResponse()?.invoke(this)
            }

            session.put(
                response.message.toSessionId(),
                MediaQuerySession(query, media?.Page?.pageInfo?.currentPage!!)
            )
        }
    }

    kord.on<ButtonInteractionCreateEvent> {

        val uri = Uri.parse(interaction.componentId)

        if (interaction.user.id != interaction.message.interaction?.user?.id) {
            return@on
        }

        if (uri.host == "next") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = session.getIfPresent(sessionId)
            if (sessionOrNull != null) {
                val page = sessionOrNull.page + 1
                val media = aniListMediaDataSource.query(sessionOrNull.query, page)

                session.put(
                    sessionId,
                    sessionOrNull.copy(page = page)
                )

                interaction.updatePublicMessage { media?.toResponse()?.invoke(this) }
            } else {
                interaction.updatePublicMessage {
                    components = mutableListOf()
                }
            }
        }
        if (uri.host == "previous") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = session.getIfPresent(sessionId)
            if (sessionOrNull != null) {
                val page = sessionOrNull.page - 1
                val media = aniListMediaDataSource.query(sessionOrNull.query, page)

                session.put(
                    sessionId,
                    sessionOrNull.copy(page = page)
                )

                interaction.updatePublicMessage { media?.toResponse()?.invoke(this) }
            } else {
                interaction.updatePublicMessage {
                    content = "Disabled interaction because session doesn't exist: $sessionId"
                    components = mutableListOf()
                }
            }
        }
    }

    kord.on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on
        if (message.content == "!ping") message.channel.createMessage("Pong!")
    }



    kord.login {
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent
    }
}