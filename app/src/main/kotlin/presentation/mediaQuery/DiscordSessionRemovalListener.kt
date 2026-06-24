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
                    rest.channel.editMessage(channelId, messageId) {
                        components = mutableListOf()
                    }
                }
                else -> {
                    val (messageId, channelId) = id ?: return@launch
                    rest.channel.editMessage(channelId, messageId) {
                        components = mutableListOf()
                    }
                }
            }
        }
    }
}