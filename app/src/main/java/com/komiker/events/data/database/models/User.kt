package com.komiker.events.data.database.models

import kotlinx.serialization.Serializable

@Serializable
data class User(

    val user_id: String,
    var name: String,
    var username: String,
    var email: String,
    var avatar: String,
    var telegram_link: String? = null,
    var instagram_link: String? = null
)