package me.ghostbear.koguma.ext

import dev.kord.core.Kord
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.Event
import io.sentry.Sentry
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

inline fun <reified T : Event> Kord.on(
    scope: CoroutineScope = this,
    noinline consumer: suspend T.() -> Unit
): Job =
    events.buffer()
        .filterIsInstance<T>()
        .onEach { event ->
            scope.launch(SentryContext() + ExceptionHandler) {
                runCatching { consumer(event) }
                    .onFailure { Sentry.captureException(it) }
            }
        }
        .launchIn(scope)
