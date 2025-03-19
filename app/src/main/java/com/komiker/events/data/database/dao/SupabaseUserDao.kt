package com.komiker.events.data.database.dao

import com.komiker.events.data.database.dao.implementation.UserDao
import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseUserDao(private val supabase: SupabaseClient) : UserDao {

    override suspend fun insertUser(user: User) {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("users").insert(user)
            } catch (e: UnknownRestException) {
                if (e.message?.contains("duplicate key value violates unique constraint") == true) {
                    println("User with UUID ${user.user_id} already exists.")
                } else {
                    throw e
                }
            } catch (e: Exception) {
                println("Error occurred: ${e.message}")
            }
        }
    }

    override suspend fun getUserById(id: String): PostgrestResult {
        return withContext(Dispatchers.IO) {
            supabase.from("users").select(columns = Columns.list("user_id", "name", "username", "email", "avatar")) {
                filter {
                    eq("user_id", id)
                }
            }
        }
    }

    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("users").update(user) {
                    filter {
                        eq("user_id", user.user_id)
                    }
                }
            } catch (e: Exception) {
                println("Error updating user: ${e.message}")
            }
        }
    }
}
