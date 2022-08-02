/*
 * Copyright (C) 2022 ghostbear
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package me.ghostbear.koguma.model


import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Manga(
    @SerialName("title")
    val title: String? = null,
    @SerialName("author")
    val author: String? = null,
    @SerialName("artist")
    val artist: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("genre")
    val genre: List<String>? = null,
    @SerialName("status")
    val status: Status? = null
)

@Serializable(with = StatusAsStringSerializer::class)
enum class Status {
    Unknown,
    Ongoing,
    Completed,
    Licensed,
    PublishingFinished,
    Cancelled,
    OnHaitus;

    companion object {

        val values = values()

        fun valueOf(ordinal: Int): Status {
            return values().find { it.ordinal == ordinal } ?: Unknown
        }
    }
}

object StatusAsStringSerializer : KSerializer<Status> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("status", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Status) {
        val string = "${value.ordinal}"
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Status {
        val string = decoder.decodeString()
        return Status.valueOf(string.toIntOrNull() ?: 0)
    }
}


