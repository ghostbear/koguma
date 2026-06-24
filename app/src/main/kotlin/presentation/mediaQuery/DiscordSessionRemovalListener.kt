package me.ghostbear.koguma.presentation.mediaQuery

import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ghostbear.koguma.di.KordContext

class DiscordSessionRemovalListener(
    private val scope: CoroutineScope,
    private val context: KordContext,
) : RemovalListener<DiscordMessageReference, DiscordSession> {
    override fun onRemoval(
        id: DiscordMessageReference?,
        session: DiscordSession?,
        cause: RemovalCause
    ) {
        if (cause == RemovalCause.REPLACED) return
        scope.launch {
            val kord = context.kord
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
}