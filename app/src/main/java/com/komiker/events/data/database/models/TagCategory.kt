package com.komiker.events.data.database.models

import com.komiker.events.data.database.converters.OffsetDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class TagCategory(

    val id: String,
    val name: String,
    @SerialName("sub_tags")
    val subTags: List<String>,
    @Serializable(with = OffsetDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: OffsetDateTime
)