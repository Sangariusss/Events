package com.komiker.events.data.database.models

import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class Proposal(

    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val username: String,
    @SerialName("user_avatar")
    val userAvatar: String?,
    val content: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime,
    @SerialName("likes_count")
    val likesCount: Int
    // val commentsCount: Int
)