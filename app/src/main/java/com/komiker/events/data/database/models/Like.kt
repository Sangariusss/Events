package com.komiker.events.data.database.models

import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class Like(

    val id: String,
    @SerialName("proposal_id")
    val proposalId: String,
    @SerialName("user_id")
    val userId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime
)