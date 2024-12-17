package com.komiker.events.data.database

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.cdimascio.dotenv.dotenv

object SupabaseClientProvider {

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    private val supabaseUrl: String = dotenv["SUPABASE_URL"] ?: throw IllegalStateException("SUPABASE_URL is not defined in .env file")
    private val supabaseKey: String = dotenv["SUPABASE_KEY"] ?: throw IllegalStateException("SUPABASE_KEY is not defined in .env file")

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseKey
    ) {
        install(Auth) {
            host = "login-callback"
            scheme = "com.events"
        }
        install(Postgrest)
        install(Storage)
    }
}