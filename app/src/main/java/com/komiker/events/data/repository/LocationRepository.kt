package com.komiker.events.data.repository

import android.content.Context
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.komiker.events.data.models.LocationItem
import com.komiker.events.utils.PlacesApiUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationRepository(context: Context) {

    private val placesClient: PlacesClient = PlacesApiUtil.initializePlaces(context)

    fun getLocations(query: String = ""): Flow<List<LocationItem>> = flow {
        val token = AutocompleteSessionToken.newInstance()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(token)
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                placesClient.findAutocompletePredictions(request).await()
            }
            val locations = response.autocompletePredictions.map { prediction ->
                LocationItem(
                    id = prediction.placeId,
                    address = prediction.getFullText(null).toString(),
                    updatedAt = ""
                )
            }
            emit(locations)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}