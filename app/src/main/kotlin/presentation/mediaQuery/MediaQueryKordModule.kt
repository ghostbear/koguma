package me.ghostbear.koguma.presentation.mediaQuery

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.embed
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import me.ghostbear.koguma.domain.mediaQuery.Media
import me.ghostbear.koguma.domain.mediaQuery.MediaDataSource
import me.ghostbear.koguma.domain.mediaQuery.MediaFormat
import me.ghostbear.koguma.domain.mediaQuery.MediaQuery
import me.ghostbear.koguma.domain.mediaQuery.MediaResult
import me.ghostbear.koguma.domain.mediaQuery.MediaStatus
import me.ghostbear.koguma.domain.mediaQuery.MediaType
import me.ghostbear.koguma.domain.mediaQueryParser.MediaQueryMatcher
import me.ghostbear.koguma.ext.nsfw
import me.ghostbear.koguma.ext.on
import me.ghostbear.koguma.ext.takeIf
import me.ghostbear.koguma.session.SessionStore

suspend fun Kord.mediaQueryModule(
    matcher: MediaQueryMatcher,
    dataSource: MediaDataSource,
    sessionStore: SessionStore<DiscordMessageReference, DiscordSession>
) {

    createGlobalChatInputCommand(
        "search",
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

        if (command.rootName == "search") {
            val deferredResponse = interaction.deferPublicResponse()
            val query = command.strings["query"]!!
            val type = when (command.strings["type"]!!) {
                "anime" -> MediaType.ANIME
                "manga" -> MediaType.MANGA
                "light_novel" -> MediaType.NOVEL
                else -> throw IllegalArgumentException("Unknown type of command")
            }

            val channel = interaction.channel.asChannel()
            val result = dataSource.query(MediaQuery(query, type, channel.nsfw))
            when (result) {
                is MediaResult.Success -> {
                    val message = deferredResponse.respond(result.messageBuilder)
                    sessionStore.put(message.message.reference(), DiscordSession.Interaction(result.mediaQuery))
                }

                is MediaResult.Error.Message -> deferredResponse.delete()
                is MediaResult.Error.NotFound -> deferredResponse.delete()
            }
        }
    }

    on<ButtonInteractionCreateEvent> {
        val componentId = interaction.componentId

        if (componentId == "next" || componentId == "previous") {
            val sessionId = interaction.message.messageReference?.reference() ?: interaction.message.reference()
            val sessionOrNull =
                sessionStore.getOrNull(sessionId) ?: return@on.also {
                    interaction.updatePublicMessage {
                        components = mutableListOf()
                    }
                }

            val direction = if (componentId == "next") 1 else -1
            val mediaQuery = when (sessionOrNull) {
                is DiscordSession.Interaction -> sessionOrNull.mediaQuery
                is DiscordSession.Message -> sessionOrNull.mediaQuery.first()
            }
            val query = mediaQuery.copy(currentPage = mediaQuery.currentPage + direction)

            val result = dataSource.query(query)
            when (result) {
                is MediaResult.Error.Message -> {}
                is MediaResult.Error.NotFound -> {}
                is MediaResult.Success -> {
                    interaction.updatePublicMessage(result.messageBuilder)
                    sessionStore.put(sessionId, DiscordSession.Interaction(result.mediaQuery))
                }
            }
        }
        if (componentId == "freeze") {
            interaction.message.edit {
                components = mutableListOf()
            }
        }
    }

    val process: suspend (Message) -> Unit = process@{ message ->
        val sessionId = message.reference()
        val activeSessionOrNull = sessionStore.getOrNull(sessionId).takeIf<DiscordSession.Message>()

        val (matches) = matcher.match(message.content)
        if (matches.isEmpty()) {
            if (activeSessionOrNull != null) {
                val (messageId, channelId) = activeSessionOrNull.replyReference
                rest.channel.deleteMessage(channelId, messageId, "User message doesn't include any search matches")
            }
            return@process
        }

        val messageBulider: MessageBuilder.(List<MediaResult<Media>>) -> Unit = builder@{ results ->
            content = null
            embeds = mutableListOf()
            components = mutableListOf()

            if (results.size == 1) {
                val result = results.first()
                when (result) {
                    is MediaResult.Success -> {
                        allowedMentions {
                            repliedUser = false
                        }
                        result.messageBuilder(this)
                    }

                    is MediaResult.Error.Message -> {
                    }

                    is MediaResult.Error.NotFound -> {
                    }
                }
                return@builder
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

        message.channel.withTyping {
            val channel = asChannel()
            val queries = matches.map { MediaQuery(it.query, it.type, channel.nsfw, it.page) }.toTypedArray()

            val results = dataSource.query(*queries)

            val replyReference = if (activeSessionOrNull != null) {
                val (messageId, channelId) = activeSessionOrNull.replyReference
                rest.channel.editMessage(channelId, messageId) {
                    allowedMentions {
                        repliedUser = false
                    }
                    messageBulider(this, results)
                }
                activeSessionOrNull.replyReference
            } else {
                message.reply {
                    allowedMentions {
                        repliedUser = false
                    }
                    messageBulider(this, results)
                }.reference()
            }

            sessionStore.put(
                sessionId,
                DiscordSession.Message(queries, replyReference)
            )
        }
    }

    on<MessageCreateEvent> {
        process(message)
    }

    on<MessageUpdateEvent> {
        process(message.asMessage())
    }
}

val MediaResult.Success.messageBuilder: MessageBuilder.() -> Unit
    get() = {
        embed(media)
        actionRow(mediaQuery)
    }

fun String.ellipsisIfNeeded(limit: Int): String {
    var count = 0
    val text = takeWhile { char ->
        if (char.isWhitespace()) {
            count++
        }
        count < limit
    }
    return "$text…".takeIf { (text.length < length) } ?: text
}

fun MessageBuilder.embed(media: Media) {
    embed {
        url = media.url
        title = media.title
        description = media.description?.ellipsisIfNeeded(64)
        color = media.color?.let { Color(it) }
        media.thumbnailUrl?.let { thumbnailUrl ->
            thumbnail {
                url = thumbnailUrl
            }
        }
        media.year?.let { year ->
            field {
                name = "Year"
                value = "$year"
                inline = true
            }
        }
        media.season?.let { season ->
            field {
                name = "Season"
                value = season.name
                inline = true
            }
        }
        media.meanScore?.let { meanScore ->
            field {
                name = "Mean Score"
                value = "$meanScore"
                inline = true
            }
        }
        media.episodeCount?.let { episodeCount ->
            field {
                name = "Episodes"
                value = "$episodeCount"
                inline = true
            }
        }
        media.chapterCount?.let { chapterCount ->
            field {
                name = "Chapters"
                value = "$chapterCount"
                inline = true
            }
        }
        media.genres?.let { genres ->
            field {
                name = "Genres"
                value = genres.joinToString(", ")
            }
        }
        footer {
            text = listOfNotNull(
                when (media.format) {
                    MediaFormat.TV -> "TV"
                    MediaFormat.TV_SHORT -> "TV Short"
                    MediaFormat.MOVIE -> "Movie"
                    MediaFormat.SPECIAL -> "Special"
                    MediaFormat.OVA -> "OVA"
                    MediaFormat.ONA -> "ONA"
                    MediaFormat.MUSIC -> "Music"
                    MediaFormat.MANGA -> "Manga"
                    MediaFormat.NOVEL -> "Novel"
                    MediaFormat.ONE_SHOT -> "One-shot"
                    null -> null
                },
                when (media.status) {
                    MediaStatus.FINISHED -> "Finished"
                    MediaStatus.RELEASING -> "Releasing"
                    MediaStatus.NOT_YET_RELEASED -> "Not yet released"
                    MediaStatus.CANCELLED -> "Cancelled"
                    MediaStatus.HIATUS -> "Hiatus"
                    null -> null
                },
                media.startDate?.format(LocalDate.Format { year(); char('/'); monthNumber(); char('/'); dayOfMonth() }),
            ).joinToString(" - ")
        }
    }
}

fun MessageBuilder.actionRow(query: MediaQuery) {
    actionRow {
        interactionButton(ButtonStyle.Primary, "previous") {
            disabled = query.currentPage <= 1
            label = "Previous"
        }
        interactionButton(ButtonStyle.Secondary, "freeze") {
            disabled = false
            label = "\u2744\uFE0F"
        }
        interactionButton(ButtonStyle.Primary, "next") {
            disabled = query.currentPage >= query.lastPage
            label = "Next"
        }
    }
}