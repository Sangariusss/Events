package com.komiker.events.data.database.dao.implementation

import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.postgrest.result.PostgrestResult

interface UserDao {

    suspend fun insertUser(user: User)
    suspend fun getUserById(id: String): PostgrestResult
}