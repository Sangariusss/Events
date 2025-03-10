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
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.data.database.entities.User
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Facebook
import kotlinx.coroutines.launch

class FacebookAuthManager {

    private val supabaseClient = SupabaseClientProvider.client
    private val userDao = SupabaseUserDao(supabaseClient)

    fun setupFacebookSignInButton(
        context: Context,
        button: Button,
        lifecycleScope: LifecycleCoroutineScope,
        navController: NavController
    ) {
        button.setOnClickListener {
            lifecycleScope.launch {
                startFacebookSignIn(context, navController)
            }
        }
    }

    private suspend fun startFacebookSignIn(context: Context, navController: NavController) {
        try {
            val authUrl = supabaseClient.auth.signInWith(Facebook) {}

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl.toString()))
            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun handleFacebookSignInResult(navController: NavController) {
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val userId = session.user?.id
                val email = session.user?.email ?: generateEmailFallback()
                val pictureUrl = session.user?.userMetadata?.get("picture")?.toString()
                    ?.replace("\"", "")
                    ?: R.drawable.img_profile_placeholder.toString()

                val user = User(
                    id_user = userId.toString(),
                    username = generateUsername(),
                    email = email,
                    avatar = pictureUrl
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
        return "facebook_user_$randomNumber@fallback.com"
    }

    private fun generateUsername(): String {
        val random = java.util.Random()
        val randomNumber = random.nextInt(900000) + 100000
        return "username$randomNumber"
    }
}