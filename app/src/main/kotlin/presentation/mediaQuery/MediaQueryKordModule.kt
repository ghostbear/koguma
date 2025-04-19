package me.ghostbear.koguma.presentation.mediaQuery

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.embed
import me.ghostbear.koguma.domain.mediaQuery.Media
import me.ghostbear.koguma.domain.mediaQuery.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.domain.session.SessionStore

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

typealias MediaQuerySessionStore = SessionStore<ChannelIdAndMessageId, MediaQuery>

suspend fun Kord.mediaQueryModule(
    matcher: MediaQueryMatcher,
    dataSource: MediaDataSource,
    sessionStore: MediaQuerySessionStore
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

            val result = dataSource.query(MediaQuery(query, type))
            when (result) {
                is MediaResult.Success -> {
                    val message = deferredResponse.respond(result.messageBuilder)
                    sessionStore.put(message.message.toSessionId(), result.mediaQuery)
                }

                is MediaResult.Error.Message -> deferredResponse.delete()
                is MediaResult.Error.NotFound -> deferredResponse.delete()
            }
        }
    }

    on<ButtonInteractionCreateEvent> {
        val componentId = interaction.componentId

        if (componentId == "next" || componentId == "previous") {
            val sessionId = interaction.message.toSessionId()
            val sessionOrNull = sessionStore.getOrNull(sessionId) ?: return@on.also {
                interaction.updatePublicMessage {
                    components = mutableListOf()
                }
            }

            val direction = if (componentId == "next") 1 else -1
            val query = sessionOrNull.copy(currentPage = sessionOrNull.currentPage + direction)

            val result = dataSource.query(query)
            when (result) {
                is MediaResult.Error.Message -> {}
                is MediaResult.Error.NotFound -> {}
                is MediaResult.Success -> {
                    interaction.updatePublicMessage(result.messageBuilder)
                    sessionStore.put(sessionId, result.mediaQuery)
                }
            }
        }
    }

    on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on

        val matches = matcher.match(message.content)
        val queries = matches.matches.map { match -> MediaQuery(match.query, match.type) }.toTypedArray()

        if (queries.isEmpty()) {
            return@on
        }
        message.channel.withTyping {

            if (queries.size == 1) {
                val mediaQuery = queries.first()

                val mediaResult = dataSource.query(mediaQuery)

                when (mediaResult) {
                    is MediaResult.Success -> {
                        val reply = message.reply {
                            allowedMentions {
                                repliedUser = false
                            }
                            mediaResult.messageBuilder(this)
                        }

                        sessionStore.put(reply.toSessionId(), mediaResult.mediaQuery)
                    }

                    is MediaResult.Error.Message -> {
                    }

                    is MediaResult.Error.NotFound -> {
                    }
                }
                return@withTyping
            }

            val results = dataSource.query(*queries)

            message.reply {
                allowedMentions {
                    repliedUser = false
                }
                content = buildString {
                    results.filterIsInstance<MediaResult.Success>()
                        .forEach { append("- [${it.media.title}](<${it.media.url}>)\n") }

                    val notFound = results.filterIsInstance<MediaResult.Error.NotFound>()
                    if (notFound.isNotEmpty()) {
                        append("Could not find\n")
                        notFound.forEach { append("- ${it.mediaQuery.query}\n") }
                    }

                    val errorMessages = results.filterIsInstance<MediaResult.Error.Message>()
                    if (errorMessages.isNotEmpty()) {
                        append("Could not retrieve\n")
                        errorMessages.forEach { append("- ${it.mediaQuery.query}\n") }
                    }
                }
            }
        }
    }
}

val MediaResult.Success.messageBuilder: MessageBuilder.() -> Unit
    get() = {
        embed(media)
        actionRow(mediaQuery)
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