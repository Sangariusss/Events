package com.komiker.events.data.repository

import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.models.TagCategory
import com.komiker.events.data.database.entities.TagCategoryEntity
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter

class TagRepository(private val database: AppDatabase) {

    private val supabaseClient = SupabaseClientProvider.client
    private lateinit var channel: RealtimeChannel

    fun getTagCategories(): Flow<List<TagCategoryEntity>> {
        return database.tagCategoryDao().getAllTagCategories()
    }

    suspend fun syncTagCategories() {
        try {
            val tagCategories = supabaseClient.from("tag_categories")
                .select()
                .decodeList<TagCategory>()
            val entities = tagCategories.map { category ->
                TagCategoryEntity(
                    id = category.id,
                    name = category.name,
                    subTags = category.subTags,
                    updatedAt = category.updatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }
            database.tagCategoryDao().deleteAll()
            database.tagCategoryDao().insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupRealtimeUpdates() {
        channel = supabaseClient.channel("tag-categories-channel")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tag_categories"
        }

        CoroutineScope(Dispatchers.IO).launch {
            changeFlow.collect { change ->
                when (change) {
                    is PostgresAction.Insert -> {
                        val record = change.record
                        val newCategory = TagCategoryEntity(
                            id = record["id"].toString(),
                            name = record["name"].toString(),
                            subTags = Json.decodeFromString<List<String>>(record["sub_tags"].toString()),
                            updatedAt = record["updated_at"].toString()
                        )
                        database.tagCategoryDao().insertAll(listOf(newCategory))
                    }
                    is PostgresAction.Update -> {
                        val record = change.record
                        val updatedCategory = TagCategoryEntity(
                            id = record["id"].toString(),
                            name = record["name"].toString(),
                            subTags = Json.decodeFromString<List<String>>(record["sub_tags"].toString()),
                            updatedAt = record["updated_at"].toString()
                        )
                        database.tagCategoryDao().insertAll(listOf(updatedCategory))
                    }
                    is PostgresAction.Delete -> {
                        database.tagCategoryDao().deleteAll()
                        syncTagCategories()
                    }
                    else -> {}
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            channel.subscribe()
        }
    }

    fun cleanupRealtime() {
        CoroutineScope(Dispatchers.IO).launch {
            supabaseClient.realtime.removeChannel(channel)
        }
    }
}