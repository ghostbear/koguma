package me.ghostbear.koguma.presentation.mediaQuery

import me.ghostbear.koguma.domain.mediaQuery.MediaQuery

sealed interface DiscordSession {
    class Interaction(val mediaQuery: MediaQuery) : DiscordSession
    class Message(val mediaQuery: Array<MediaQuery>, val replyReference: DiscordMessageReference) : DiscordSession
}