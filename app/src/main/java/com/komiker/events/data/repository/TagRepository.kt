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
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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

    fun setupRealtimeUpdates(coroutineScope: CoroutineScope) {
        channel = supabaseClient.channel("tag-categories-channel")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "tag_categories"
        }

        coroutineScope.launch {
            changeFlow.collect { change ->
                when (change) {
                    is PostgresAction.Insert -> {
                        val category = createTagCategoryEntity(change.record)
                        database.tagCategoryDao().insertAll(listOf(category))
                    }
                    is PostgresAction.Update -> {
                        val category = createTagCategoryEntity(change.record)
                        database.tagCategoryDao().insertAll(listOf(category))
                    }
                    is PostgresAction.Delete -> {
                        database.tagCategoryDao().deleteAll()
                        syncTagCategories()
                    }
                    else -> {}
                }
            }
        }

        coroutineScope.launch {
            channel.subscribe()
        }
    }

    fun cleanupRealtime() {
        coroutineScope.launch {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    private fun createTagCategoryEntity(record: Map<String, Any?>): TagCategoryEntity {
        return TagCategoryEntity(
            id = record["id"].toString(),
            name = record["name"].toString(),
            subTags = Json.decodeFromString<List<String>>(record["sub_tags"].toString()),
            updatedAt = record["updated_at"].toString()
        )
    }
}