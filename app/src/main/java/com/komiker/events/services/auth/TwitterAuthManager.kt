package com.komiker.events.services.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Button
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.data.database.models.User
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
    ) {
        button.setOnClickListener {
            lifecycleScope.launch {
                startTwitterSignIn(context)
            }
        }
    }

    private suspend fun startTwitterSignIn(context: Context) {
        try {
            val authUrl = supabaseClient.auth.signInWith(Twitter) {}
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl.toString()))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    suspend fun handleTwitterSignInResult(navController: NavController) {
        try {
            val session = supabaseClient.auth.currentSessionOrNull() ?: return
            val userId = session.user?.id ?: return
            val email = session.user?.email ?: generateEmailFallback()
            val pictureUrl = session.user?.userMetadata?.get("picture")?.toString()
                ?.replace("\"", "")
                ?: R.drawable.img_profile_placeholder.toString()

            val user = User(
                user_id = userId,
                name = generateUsername(),
                username = generateUsername(),
                email = email,
                avatar = pictureUrl
            )
            userDao.insertUser(user)

            val fadeOutAnimation = R.anim.fade_out
            val fadeInAnimation = R.anim.fade_in

            navController.navigate(
                R.id.RegistrationSuccessFragment,
                null,
                NavOptions.Builder()
                    .setEnterAnim(fadeInAnimation)
                    .setExitAnim(fadeOutAnimation)
                    .setPopUpTo(R.id.WelcomeFragment, true)
                    .build()
            )
        } catch (_: Exception) {}
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