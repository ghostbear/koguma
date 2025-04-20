package me.ghostbear.koguma.presentation.mediaQuery

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.entity.MessageReference

data class DiscordMessageReference(
    val messageId: Snowflake,
    val channelId: Snowflake,
)

fun MessageBehavior.reference(): DiscordMessageReference {
    return DiscordMessageReference(id, channelId)
}

fun MessageReference.reference(): DiscordMessageReference? {
    return message?.reference()
}

