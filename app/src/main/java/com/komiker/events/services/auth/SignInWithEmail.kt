package com.komiker.events.services.auth

import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.OTP

class SignInWithEmail(private val supabase: SupabaseClient, private val mail: String) {

    private val userDao = SupabaseUserDao(supabase)
    private val supabaseClient = SupabaseClientProvider.client

    suspend fun signIn(): Boolean {
        return try {
            supabase.auth.signInWith(OTP) {
                this.email = mail
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verifyOtp(token: String): Boolean {
        return try {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = mail,
                token = token
            )
            val session = supabaseClient.auth.currentSessionOrNull()
            val userId = session?.user?.id
            val user = User(
                id_user = userId.toString(),
                generateUsername(),
                mail,
                R.drawable.img_profile_placeholder.toString()
            )
            userDao.insertUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateUsername(): String {
        val random = java.util.Random()
        val randomNumber = random.nextInt(900000) + 100000
        return "username$randomNumber"
    }
}