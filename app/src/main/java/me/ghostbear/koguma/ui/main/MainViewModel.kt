/*
 * Copyright (C) 2022 ghostbear
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package me.ghostbear.koguma.ui.main

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import me.ghostbear.koguma.model.Manga
import me.ghostbear.koguma.model.Status
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val json: Json
) : ViewModel() {

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events = _events.receiveAsFlow()

    var openUri: Uri? by mutableStateOf(null)

    var title: String? by mutableStateOf(null)
    var author: String? by mutableStateOf(null)
    var artist: String? by mutableStateOf(null)
    var description: String? by mutableStateOf(null)
    var genre: String? by mutableStateOf(null)
    var status: Status? by mutableStateOf(null)

    val isSavable by derivedStateOf { listOf(title, author, artist, description, genre, status).any { it != null } }

    suspend fun load(inputStream: InputStream) = withContext(Dispatchers.IO) {
        try {
            val manga = json.decodeFromStream<Manga>(inputStream)
            title = manga.title
            author = manga.author
            artist = manga.artist
            description = manga.description
            genre = manga.genre?.joinToString()
            status = manga.status
            _events.trySend(Event.SuccessReadingFile)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error reading manga detail from file", e)
            _events.trySend(Event.FailureReadingFile(e))
        }
    }

    suspend fun save(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        try {
            json.encodeToStream(
                Manga(
                    title,
                    author,
                    artist,
                    description,
                    genre?.split(",\\s*".toRegex()),
                    status
                ),
                outputStream
            )
            _events.trySend(Event.SuccessWritingFile)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error writing manga detail from file", e)
            _events.trySend(Event.FailureWritingFile(e))
        }
    }

    sealed class Event {
        object SuccessReadingFile : Event()
        data class FailureReadingFile(val error: Throwable) : Event()
        object SuccessWritingFile : Event()
        data class FailureWritingFile(val error: Throwable) : Event()
    }
}