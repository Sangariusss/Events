package com.komiker.events.data.database.models

import android.os.Parcelable
import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Parcelize
@Serializable
data class Event(

    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val username: String,
    @SerialName("user_avatar")
    val userAvatar: String?,
    val title: String?,
    val description: String?,
    @SerialName("start_date")
    val startDate: String?,
    @SerialName("end_date")
    val endDate: String?,
    @SerialName("event_time")
    val eventTime: String?,
    val tags: List<String>?,
    val location: String?,
    val images: List<String>?,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: OffsetDateTime?,
    @SerialName("likes_count")
    val likesCount: Int,
    @SerialName("views_count")
    val viewsCount: Int = 0
) : Parcelable