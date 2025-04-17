package me.ghostbear.koguma.presentation.mediaQuery

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import me.ghostbear.koguma.domain.mediaQuery.Media
import me.ghostbear.koguma.domain.mediaQuery.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaQuerySessionizer
import me.ghostbear.koguma.domain.mediaQuery.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.MediaType

data class ChannelIdAndMessageId(
    val channelId: Snowflake,
    val messageId: Snowflake,
)

fun Message.toSessionId(): ChannelIdAndMessageId {
    return ChannelIdAndMessageId(
        channelId = channelId,
        messageId = id
    )
}

suspend fun Kord.mediaQueryModule(
    dataSource: MediaDataSource,
    sessionizer: MediaQuerySessionizer<ChannelIdAndMessageId>
) {

    createGlobalChatInputCommand(
        "ani-search",
        "Search for an anime, manga, or light novels",
    ) {
        string("query", "Search query") {
            required = true
        }
        string("type", "The type of media") {
            required = true
            choice("Anime", "anime")
            choice("Manga", "manga")
            choice("Light Novel", "light_novel")
        }
    }

    on<ChatInputCommandInteractionCreateEvent> {
        val command = interaction.command

        if (command.rootName == "ani-search") {
            val deferredResponse = interaction.deferPublicResponse()
            val query = command.strings["query"]!!
            val type = when (command.strings["type"]!!) {
                "anime" -> MediaType.ANIME
                "manga" -> MediaType.MANGA
                "light_novel" -> MediaType.NOVEL
                else -> throw IllegalArgumentException("Unknown type of command")
            }

            val mediaResult = dataSource.query(MediaQuery(query, type))
            when (mediaResult) {
                is MediaResult.Error -> deferredResponse.delete()
                is MediaResult.Success<*> -> {
                    val response = deferredResponse.respond {
                        with(mediaResult.media, mediaResult.mediaQuery)
                    }

                    sessionizer.put(
                        response.message.toSessionId(),
                        mediaResult.mediaQuery
                    )
                }
            }
        }
    }

    on<ButtonInteractionCreateEvent> {
        val componentId = interaction.componentId

        if (componentId == "next") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = sessionizer.getOrNull(sessionId)
            if (sessionOrNull != null) {
                val query = sessionOrNull.copy(currentPage = sessionOrNull.currentPage + 1)
                val mediaResult = dataSource.query(query)
                when (mediaResult) {
                    is MediaResult.Error -> {}
                    is MediaResult.Success<*> -> {
                        interaction.updatePublicMessage {
                            with(mediaResult.media, mediaResult.mediaQuery)
                        }

                        sessionizer.put(sessionId, mediaResult.mediaQuery)
                    }
                }
            } else {
                interaction.updatePublicMessage {
                    components = mutableListOf()
                }
            }
        }
        if (componentId == "previous") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = sessionizer.getOrNull(sessionId)
            if (sessionOrNull != null) {
                val query = sessionOrNull.copy(currentPage = sessionOrNull.currentPage - 1)
                val mediaResult = dataSource.query(query)
                when (mediaResult) {
                    is MediaResult.Error -> {}
                    is MediaResult.Success<*> -> {
                        interaction.updatePublicMessage {
                            with(mediaResult.media, mediaResult.mediaQuery)
                        }

                        sessionizer.put(sessionId, mediaResult.mediaQuery)
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

}

fun MessageBuilder.with(media: Media, query: MediaQuery) {
    embed(media)
    actionRow(query)
}

fun MessageBuilder.embed(media: Media) {
    embed {
        title = media.title
        description = media.description
        thumbnail {
            url = media.thumbnailUrl
        }
        field {
            name = "Type"
            value = media.type.name
            inline = true
        }
        field {
            name = "Year"
            value = "${media.year}"
            inline = true
        }
        field {
            name = "Mean Score"
            value = "${media.meanScore}"
            inline = true
        }
        field {
            name = "Genres"
            value = media.genres.joinToString(", ")
        }
    }
}

fun MessageBuilder.actionRow(query: MediaQuery) {
    actionRow {
        interactionButton(ButtonStyle.Primary, "previous") {
            disabled = query.currentPage <= 1
            label = "Previous"
        }
        interactionButton(ButtonStyle.Secondary, "void") {
            disabled = true
            label = "${query.currentPage}/${query.lastPage}"
        }
        interactionButton(ButtonStyle.Primary, "next") {
            disabled = query.currentPage >= query.lastPage
            label = "Next"
        }
    }
}