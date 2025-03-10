package com.komiker.events.services.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Button
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Twitter
import kotlinx.coroutines.launch

class TwitterAuthManager {

    private val supabaseClient = SupabaseClientProvider.client
    private val userDao = SupabaseUserDao(supabaseClient)

    fun setupTwitterSignInButton(
        context: Context,
        button: Button,
        lifecycleScope: LifecycleCoroutineScope,
        navController: NavController
    ) {
        button.setOnClickListener {
            lifecycleScope.launch {
                startTwitterSignIn(context, navController)
            }
        }
    }

    private suspend fun startTwitterSignIn(context: Context, navController: NavController) {
        try {
            val authUrl = supabaseClient.auth.signInWith(Twitter) {}

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl.toString()))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun handleTwitterSignInResult(navController: NavController) {
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val userId = session.user?.id
                val email = session.user?.email ?: generateEmailFallback()
                val username = session.user?.userMetadata?.get("preferred_username")?.toString() ?: generateUsername()
                val avatar = session.user?.userMetadata?.get("avatar_url")?.toString()
                    ?: R.drawable.img_profile_placeholder.toString()

                val user = User(
                    id_user = userId.toString(),
                    username = username,
                    email = email,
                    avatar = avatar
                )
                userDao.insertUser(user)

                val fadeOutAnimation = R.anim.fade_out
                val fadeInAnimation = R.anim.fade_in

                navController.navigate(
                    R.id.action_WelcomeFragment_to_RegistrationSuccessFragment,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(fadeInAnimation)
                        .setExitAnim(fadeOutAnimation)
                        .build()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateEmailFallback(): String {
        val random = java.util.Random()
        val randomNumber = random.nextInt(900000) + 100000
        return "twitter_user_$randomNumber@fallback.com"
    }

    private fun generateUsername(): String {
        val random = java.util.Random()
        val randomNumber = random.nextInt(900000) + 100000
        return "username$randomNumber"
    }
}