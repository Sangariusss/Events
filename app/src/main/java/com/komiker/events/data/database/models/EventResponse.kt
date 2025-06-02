package com.komiker.events.data.database.models

import com.komiker.events.data.database.converters.NullableListSerializer
import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class EventResponse(

    val id: String?,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val username: String,
    @SerialName("user_avatar") val userAvatar: String?,
    val title: String,
    val description: String?,
    @SerialName("start_date") val startDate: String?,
    @SerialName("end_date") val endDate: String?,
    @SerialName("event_time") val eventTime: String?,
    @Serializable(with = NullableListSerializer::class) val tags: List<String>?,
    val location: String?,
    @Serializable(with = NullableListSerializer::class) val images: List<String>?,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at") val createdAt: OffsetDateTime?,
    @SerialName("likes_count") val likesCount: Int,
    @SerialName("is_liked") val isLiked: Boolean? = null
)