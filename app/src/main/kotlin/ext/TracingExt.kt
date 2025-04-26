package me.ghostbear.koguma.ext

import io.sentry.Sentry
import io.sentry.SpanStatus

inline fun <reified T> trace(name: String, operation: String, block: () -> T): T {
    val span = Sentry.getSpan() ?: Sentry.startTransaction(name, operation)
    val child = span.startChild(operation)
    try {
        return block()
    } catch (e: Exception) {
        child.throwable = e
        child.status = SpanStatus.UNKNOWN_ERROR
        throw e
    } finally {
        child.finish()
    }
}
