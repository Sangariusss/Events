package com.komiker.events.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(

    @PrimaryKey val id: String,
    val address: String,
    val updatedAt: String
)