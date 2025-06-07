package com.komiker.events.data.database.dao.implementation

import com.komiker.events.data.database.SupabaseClientProvider.client
import com.komiker.events.data.database.dao.UserDao
import com.komiker.events.data.database.models.Event
import com.komiker.events.data.database.models.EventLike
import com.komiker.events.data.database.models.EventResponse
import com.komiker.events.data.database.models.Proposal
import com.komiker.events.data.database.models.ProposalLike
import com.komiker.events.data.database.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
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

    suspend fun getEventById(eventId: String): Event? {
        return try {
            client.from("events").select {
                filter { eq("id", eventId) }
            }.decodeSingleOrNull<Event>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getProposalById(proposalId: String): Proposal? {
        return try {
            client.from("proposals").select {
                filter { eq("id", proposalId) }
            }.decodeSingleOrNull<Proposal>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getLikedEvents(userId: String): List<EventResponse> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.postgrest.rpc(
                    "get_liked_events_by_user",
                    mapOf("user_id_input" to userId)
                ).decodeList()
            } catch (e: Exception) {
                println("Error fetching liked events: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun insertProposalLike(proposalId: String, userId: String) {
        withContext(Dispatchers.IO) {
            supabase.from("proposal_likes").insert(
                mapOf(
                    "proposal_id" to proposalId,
                    "user_id" to userId
                )
            )
        }
    }

    suspend fun deleteProposalLike(proposalId: String, userId: String) {
        withContext(Dispatchers.IO) {
            supabase.from("proposal_likes").delete {
                filter {
                    eq("proposal_id", proposalId)
                    eq("user_id", userId)
                }
            }
        }
    }

    suspend fun isProposalLiked(proposalId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            supabase.from("proposal_likes").select {
                filter {
                    eq("proposal_id", proposalId)
                    eq("user_id", userId)
                }
            }.decodeList<ProposalLike>().isNotEmpty()
        }
    }

    suspend fun getProposalLikesCount(proposalId: String): Int {
        return withContext(Dispatchers.IO) {
            val proposal = supabase.from("proposals").select {
                filter { eq("id", proposalId) }
            }.decodeSingleOrNull<Proposal>()
            proposal?.likesCount ?: 0
        }
    }

    suspend fun insertEventLike(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            supabase.from("event_likes").insert(
                mapOf(
                    "event_id" to eventId,
                    "user_id" to userId
                )
            )
        }
    }

    suspend fun deleteEventLike(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            supabase.from("event_likes").delete {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }
        }
    }

    suspend fun isEventLiked(eventId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            supabase.from("event_likes").select {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", userId)
                }
            }.decodeList<EventLike>().isNotEmpty()
        }
    }

    suspend fun getEventLikesCount(eventId: String): Int {
        return withContext(Dispatchers.IO) {
            val event = supabase.from("events").select {
                filter { eq("id", eventId) }
            }.decodeSingleOrNull<Event>()
            event?.likesCount ?: 0
        }
    }
}