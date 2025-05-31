package com.komiker.events.utils

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import io.github.cdimascio.dotenv.dotenv
import java.util.Locale

object PlacesApiUtil {

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
        ignoreIfMissing = true
    }

    private val mapsApiKey: String = dotenv["MAPS_API_KEY"] ?: throw IllegalStateException("MAPS_API_KEY is not defined in .env file")

    fun initializePlaces(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, mapsApiKey, Locale.US)
        }
        return Places.createClient(context)
    }
}