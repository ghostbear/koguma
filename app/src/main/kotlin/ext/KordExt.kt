package me.ghostbear.koguma.ext

import dev.kord.core.entity.channel.Channel

val Channel.nsfw: Boolean
    get() = data.nsfw.orElse(false)