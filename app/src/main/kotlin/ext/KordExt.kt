package me.ghostbear.koguma.ext

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.Event
import dev.kord.rest.builder.message.MessageBuilder
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


val ExceptionHandler = CoroutineExceptionHandler { _, exception ->
    Sentry.captureException(exception)
}

val Channel.nsfw: Boolean
    get() = data.nsfw.orElse(false)

suspend fun Message.createOrEditReply(
    channelId: Snowflake,
    messageId: Snowflake?,
    referenceMessageId: Snowflake?,
    builder: MessageBuilder.() -> Unit
): Message {
    val message = if (messageId != null) {
        kord.rest.channel.editMessage(channelId, messageId, builder)
    } else {
        kord.rest.channel.createMessage(channelId) {
            builder()
            messageReference = referenceMessageId
        }
    }
    val data = MessageData.from(message)
    return Message(data, kord)
}

suspend fun Message.deleteOwnReactions(): Unit {
    reactions
        .filter { it.selfReacted }
        .forEach {
            deleteOwnReaction(it.emoji)
        }
}

inline fun <reified T : Event> Kord.on(
    scope: CoroutineScope = this,
    name: String = "event",
    operation: String = T::class.simpleName ?: "event",
    noinline consumer: suspend T.() -> Unit
): Job =
    events.buffer()
        .filterIsInstance<T>()
        .onEach { event ->
            scope.launch(SentryContext() + ExceptionHandler) {
                val txOptions = TransactionOptions()
                txOptions.isBindToScope = true
                val tx = Sentry.startTransaction(name, operation, txOptions)
                runCatching { consumer(event) }
                    .onFailure {
                        tx.throwable = it
                        tx.status = SpanStatus.UNKNOWN_ERROR
                    }
                tx.finish()
            }
        }
        .launchIn(scope)
