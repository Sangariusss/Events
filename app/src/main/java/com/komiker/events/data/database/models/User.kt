package com.komiker.events.data.database.models

import kotlinx.serialization.Serializable

@Serializable
data class User(

    val user_id: String,
    var name: String,
    var username: String,
    var email: String,
    var avatar: String,
)