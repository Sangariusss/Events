package com.komiker.events.data.database.models

import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class EventView(

    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("viewed_at") val viewedAt: OffsetDateTime
)