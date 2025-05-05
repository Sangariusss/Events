package com.komiker.events.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_categories")
data class TagCategoryEntity(

    @PrimaryKey val id: String,
    val name: String,
    val subTags: List<String>,
    val updatedAt: String
)