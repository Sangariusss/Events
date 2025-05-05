package com.komiker.events.data.database.dao

import com.komiker.events.data.database.models.User
import io.github.jan.supabase.postgrest.result.PostgrestResult

interface UserDao {

    suspend fun insertUser(user: User)
    suspend fun getUserById(id: String): PostgrestResult
}