package com.komiker.events.data.database.dao.implementation

import com.komiker.events.data.database.dao.UserDao
import com.komiker.events.data.database.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.gotrue.auth
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
            supabase.from("users").select(columns = Columns.list("user_id", "name", "username", "email", "avatar", "telegram_link", "instagram_link")) {
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

    suspend fun updateUserSocialLinks(userId: String, telegramLink: String?, instagramLink: String?) {
        withContext(Dispatchers.IO) {
            try {
                supabase.from("users").update(
                    mapOf(
                        "telegram_link" to telegramLink,
                        "instagram_link" to instagramLink
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            } catch (e: Exception) {
                println("Error updating social links: ${e.message}")
            }
        }
    }

    suspend fun updateEmail(userId: String, newEmail: String) {
        withContext(Dispatchers.IO) {
            try {
                supabase.auth.updateUser {
                    email = newEmail
                }
                supabase.from("users").update(
                    mapOf("email" to newEmail)
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
            } catch (e: Exception) {
                println("Error updating email: ${e.message}")
                throw e
            }
        }
    }
}
