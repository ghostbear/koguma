package me.ghostbear.koguma.presentation.mediaQuery

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.behavior.interaction.updatePublicMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.embed
import io.sentry.Sentry
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
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
import me.ghostbear.koguma.ext.createOrEditReply
import me.ghostbear.koguma.ext.deleteOwnReactions
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
        Sentry.addBreadcrumb("${interaction.channelId.value}-${interaction.command.rootName}")
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

            Sentry.configureScope { scope ->
                scope.setExtra("query", query)
                scope.setExtra("type", type.name)
                scope.setExtra("nsfw", channel.nsfw.toString())
            }

            when (result) {
                is MediaResult.Success -> {
                    val message = deferredResponse.respond(result.messageBuilder)
                    sessionStore.put(message.message.reference(), DiscordSession.Interaction(result.mediaQuery))
                }

                is MediaResult.NotFound -> {
                    val message = deferredResponse.respond {
                        content = "❓ Couldn't find any results."
                        if (!channel.nsfw) {
                            content += "\n\n\uD83D\uDEE1\uFE0F This is a SFW channel, hentai and ecchi was filtered out."
                        }
                    }
                    delay(2.seconds)
                    message.delete()
                }

                is MediaResult.Failure -> {
                    val message = deferredResponse.respond {
                        content = "\uD83D\uDD25 Something went wrong, the turd was sampled and sent for analysis."
                    }
                    delay(2.seconds)
                    message.delete()
                }
            }
        }
    }

    on<ButtonInteractionCreateEvent> {
        Sentry.addBreadcrumb("${interaction.message.channelId.value}-${interaction.message.id.value}")
        val componentId = interaction.componentId

        interaction.message.deleteOwnReactions()

        if (componentId == "next" || componentId == "previous") {
            val sessionId = interaction.message.messageReference?.reference() ?: interaction.message.reference()
            val sessionOrNull = sessionStore.getOrNull(sessionId)
            if (sessionOrNull == null) {
                interaction.updatePublicMessage {
                    components = mutableListOf()
                }
                return@on
            }

            val direction = if (componentId == "next") 1 else -1
            val mediaQuery = when (sessionOrNull) {
                is DiscordSession.Interaction -> sessionOrNull.mediaQuery
                is DiscordSession.Message -> sessionOrNull.mediaQuery.first()
            }
            val query = mediaQuery.copy(currentPage = mediaQuery.currentPage + direction)

            val result = dataSource.query(query)
            when (result) {
                is MediaResult.Failure -> {
                    interaction.message.addReaction(ReactionEmoji.Unicode("\uD83D\uDD25"))
                }

                is MediaResult.NotFound -> {
                    val channel = interaction.getChannel()
                    if (!channel.nsfw) {
                        interaction.message.addReaction(ReactionEmoji.Unicode("\uD83D\uDEE1\uFE0F"))
                    }
                    interaction.message.addReaction(ReactionEmoji.Unicode("\u2753"))
                }

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

        Sentry.configureScope { scope ->
            scope.setExtra("content", message.content)
        }

        val (matches) = matcher.match(message.content)
        if (matches.isEmpty()) {
            if (activeSessionOrNull != null) {
                val (messageId, channelId) = activeSessionOrNull.replyReference
                rest.channel.deleteMessage(channelId, messageId, "User message doesn't include any search matches")
            }
            return@process
        }

        message.channel.withTyping {
            val channel = asChannel()
            val queries = matches.map { MediaQuery(it.query, it.type, channel.nsfw, it.page) }.toTypedArray()

            val results = dataSource.query(*queries)

            val channelId = activeSessionOrNull?.replyReference?.channelId ?: id
            val replyMessageId = activeSessionOrNull?.replyReference?.messageId
            val referenceMessageId = sessionId.messageId.takeIf { activeSessionOrNull == null }

            message.deleteOwnReactions()

            val result = results.first().takeIf { results.size == 1 }
            val reply = when {
                result is MediaResult.Success -> {
                    message.createOrEditReply(channelId, replyMessageId, referenceMessageId) {
                        defaultBuilder()
                        result.messageBuilder(this)
                    }
                }

                result is MediaResult.NotFound || results.all { it is MediaResult.NotFound } -> {
                    activeSessionOrNull?.let { session ->
                        channel.deleteMessage(
                            session.replyReference.messageId,
                            "User message doesn't include any search matches"
                        )
                        sessionStore.remove(sessionId)
                    }
                    val referenceMessage = channel.getMessage(sessionId.messageId)
                    if (!channel.nsfw) {
                        referenceMessage.addReaction(ReactionEmoji.Unicode("\uD83D\uDEE1\uFE0F"))
                    }
                    referenceMessage.addReaction(ReactionEmoji.Unicode("\u2753"))
                    null
                }

                result is MediaResult.Failure || results.all { it is MediaResult.Failure } -> {
                    activeSessionOrNull?.let { session ->
                        channel.deleteMessage(
                            session.replyReference.messageId,
                            "User message doesn't include any search matches"
                        )
                        sessionStore.remove(sessionId)
                    }
                    val referenceMessage = channel.getMessage(sessionId.messageId)
                    referenceMessage.addReaction(ReactionEmoji.Unicode("\uD83D\uDD25"))
                    null
                }

                else -> {
                    message.createOrEditReply(channelId, replyMessageId, referenceMessageId) {
                        defaultBuilder()
                        content = buildString {
                            results.filterIsInstance<MediaResult.Success>()
                                .forEach { this.append("- [${it.media.title}](<${it.media.url}>)\n") }

                            val notFound = results.filterIsInstance<MediaResult.NotFound>()
                            if (notFound.isNotEmpty()) {
                                this.append("Could not find\n")
                                notFound.forEach { this.append("- ${it.mediaQuery.query}\n") }
                            }

                            val errorMessages = results.filterIsInstance<MediaResult.Failure>()
                            if (errorMessages.isNotEmpty()) {
                                this.append("Could not retrieve\n")
                                errorMessages.forEach { this.append("- ${it.mediaQuery.query}\n") }
                            }
                        }
                    }

                }
            }
            if (reply != null) {
                sessionStore.put(
                    sessionId,
                    DiscordSession.Message(queries, reply.reference())
                )
            }
        }
    }

    on<MessageCreateEvent> {
        Sentry.addBreadcrumb("${message.channelId.value}-${message.id.value}")
        process(message)
    }

    on<MessageUpdateEvent> {
        Sentry.addBreadcrumb("${message.channelId.value}-${message.id.value}")
        process(getMessage())
    }
}

val defaultBuilder: MessageBuilder.() -> Unit = {
    content = null
    embeds = mutableListOf()
    components = mutableListOf()
    allowedMentions {
        repliedUser = false
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
        when {
            media.imageUrl != null -> image = media.imageUrl
            media.thumbnailUrl != null -> thumbnail { url = media.thumbnailUrl!! }
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