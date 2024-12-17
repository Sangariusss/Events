package com.komiker.events.data.database.entities

import kotlinx.serialization.Serializable

@Serializable
data class User(

    val id_user: String,
    var username: String,
    var email: String,
    var avatar: String,
)