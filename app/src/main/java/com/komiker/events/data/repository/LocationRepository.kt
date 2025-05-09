package com.komiker.events.data.repository

import com.komiker.events.data.database.AppDatabase
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.entities.LocationEntity
import com.komiker.events.data.database.models.Location
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
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

class LocationRepository(private val database: AppDatabase) {

    private val supabaseClient = SupabaseClientProvider.client
    private lateinit var channel: RealtimeChannel
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun getLocations(): Flow<List<LocationEntity>> {
        return database.locationDao().getAllLocations()
    }

    suspend fun syncLocations() {
        try {
            val locations = supabaseClient.from("locations")
                .select()
                .decodeList<Location>()
            val entities = locations.map { location ->
                LocationEntity(
                    id = location.id,
                    address = location.address.trim('\'', '"'),
                    updatedAt = location.updatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }
            database.locationDao().deleteAll()
            database.locationDao().insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setupRealtimeUpdates(coroutineScope: CoroutineScope) {
        channel = supabaseClient.channel("locations-channel")

        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "locations"
        }

        coroutineScope.launch {
            changeFlow.collect { change ->
                when (change) {
                    is PostgresAction.Insert -> {
                        val location = createLocationEntity(change.record)
                        withContext(Dispatchers.IO) {
                            database.locationDao().insertAll(listOf(location))
                        }
                    }
                    is PostgresAction.Update -> {
                        val location = createLocationEntity(change.record)
                        withContext(Dispatchers.IO) {
                            database.locationDao().insertAll(listOf(location))
                        }
                    }
                    is PostgresAction.Delete -> {
                        withContext(Dispatchers.IO) {
                            database.locationDao().deleteAll()
                            syncLocations()
                        }
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

    private fun createLocationEntity(record: Map<String, Any?>): LocationEntity {
        val address = record["address"]?.toString()?.trim('\'', '"') ?: ""
        return LocationEntity(
            id = record["id"]?.toString() ?: "",
            address = address,
            updatedAt = record["updated_at"]?.toString() ?: ""
        )
    }
}