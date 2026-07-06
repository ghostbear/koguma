package me.ghostbear.koguma.presentation.mediaQuery

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.builder.components.emoji
import dev.kord.core.cache.data.toData
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.component.actionRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ghostbear.koguma.di.KordContext

class DiscordSessionRemovalListener(
    private val scope: CoroutineScope,
    private val context: KordContext,
) : RemovalListener<DiscordMessageReference, DiscordSession> {

    private val rest by lazy { context.rest }

    override fun onRemoval(
        id: DiscordMessageReference?,
        session: DiscordSession?,
        cause: RemovalCause
    ) {
        if (cause == RemovalCause.REPLACED) return
        scope.launch {
            when {
                session is DiscordSession.Message -> {
                    val (messageId, channelId) = session.replyReference
                    val discordMessage = rest.channel.getMessage(channelId, messageId)
                    val message = Message(discordMessage.toData(), context.kord)
                    rest.channel.editMessage(channelId, messageId) {
                        val linkButtons = message.buttons.filter { it.style == ButtonStyle.Link }
                        if (linkButtons.isNotEmpty()) {
                            actionRow {
                                linkButtons.forEach { button ->
                                    linkButton(
                                        button.url!!
                                    ) {
                                        label = button.label
                                        emoji(button.emoji as ReactionEmoji.Unicode)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val (messageId, channelId) = id ?: return@launch
                    val discordMessage = rest.channel.getMessage(channelId, messageId)
                    val message = Message(discordMessage.toData(), context.kord)
                    rest.channel.editMessage(channelId, messageId) {
                        val linkButtons = message.buttons.filter { it.style == ButtonStyle.Link }
                        if (linkButtons.isNotEmpty()) {
                            actionRow {
                                linkButtons.forEach { button ->
                                    linkButton(
                                        button.url!!
                                    ) {
                                        label = button.label
                                        emoji(button.emoji as ReactionEmoji.Unicode)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}