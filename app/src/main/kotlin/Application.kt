package me.ghostbear.koguma

import com.eygraber.uri.Uri
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.messageFlags
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

val logger = LoggerFactory.getLogger("me.ghostbear.koguma.ApplicationKt")

suspend fun main(args: Array<String>) {
    val kord = Kord(args.firstOrNull() ?: error("Missing required argument 'token'"))

    val session: Cache<MediaSessionId, MediaQuerySession> = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .maximumSize(2)
        .removalListener<MediaSessionId, MediaQuerySession> { id, session, cause ->
            logger.info("Removed session $id")
            kord.launch {
                val (channelId, messageId) = id!!
                kord.rest.channel.editMessage(channelId, messageId) {
                    components = mutableListOf()
                }
            }
        }
        .build()

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

            val response = deferredResponse.respond {
                content = "Will search for \"${query}\""
                actionRow {
                    interactionButton(ButtonStyle.Primary, "koguma://previous") {
                        disabled = true
                        label = "Previous"
                    }
                    interactionButton(ButtonStyle.Secondary, "koguma://nothing") {
                        disabled = true
                        label = "1/10"
                    }
                    interactionButton(ButtonStyle.Primary, "koguma://next") {
                        disabled = false
                        label = "Next"
                    }
                }
            }

            session.put(
                response.message.toSessionId(),
                MediaQuerySession(query)
            )
        }
    }

    kord.on<ButtonInteractionCreateEvent> {

        val uri = Uri.parse(interaction.componentId)

        if (interaction.user.id != interaction.message.interaction?.user?.id)
        {
            return@on
        }

        if (uri.host == "next") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = session.getIfPresent(sessionId)
            if (sessionOrNull != null) {
                interaction.updatePublicMessage {
                    content = "Fetching next page for: ${sessionOrNull.query}"
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "koguma://previous") {
                            disabled = false
                            label = "Previous"
                        }
                        interactionButton(ButtonStyle.Secondary, "koguma://nothing") {
                            disabled = true
                            label = "1/10"
                        }
                        interactionButton(ButtonStyle.Primary, "koguma://next") {
                            disabled = true
                            label = "Next"
                        }
                    }
                }
            } else {
                interaction.updatePublicMessage {
                    content = "Disabled interaction because session doesn't exist: $sessionId"
                    components = mutableListOf()
                }
            }
        }
        if (uri.host == "previous") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = session.getIfPresent(sessionId)
            if (sessionOrNull != null) {
                interaction.updatePublicMessage {
                    content = "Fetching previous page for: ${sessionOrNull.query}"
                    actionRow {
                        interactionButton(ButtonStyle.Primary, "koguma://previous") {
                            disabled = true
                            label = "Previous"
                        }
                        interactionButton(ButtonStyle.Secondary, "koguma://nothing") {
                            disabled = true
                            label = "1/10"
                        }
                        interactionButton(ButtonStyle.Primary, "koguma://next") {
                            disabled = false
                            label = "Next"
                        }
                    }
                }
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