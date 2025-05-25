package com.komiker.events.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.implementation.SupabaseUserDao
import com.komiker.events.databinding.ActivityMainBinding
import com.komiker.events.services.auth.FacebookAuthManager
import com.komiker.events.services.auth.TwitterAuthManager
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val facebookAuthManager = FacebookAuthManager()
    private val twitterAuthManager = TwitterAuthManager()
    private val profileViewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(supabaseUserDao) }
    private var isSocialAuthHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBindingAndNavigation()
        lifecycleScope.launch { handleAppStartup() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch { handleAppStartup() }
    }

    private fun initializeBindingAndNavigation() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_nav_host_content_main) as NavHostFragment
        navController = navHostFragment.navController
    }

    private suspend fun handleAppStartup() {
        isSocialAuthHandled = false
        val isSocialAuthIntent = processDeepLinks(intent)

        val session = withTimeoutOrNull(1000) {
            var currentSession = supabaseClient.auth.currentSessionOrNull()
            while (currentSession == null && isSocialAuthIntent) {
                delay(50)
                currentSession = supabaseClient.auth.currentSessionOrNull()
            }
            currentSession
        } ?: supabaseClient.auth.currentSessionOrNull()

        if (session?.user != null) {
            if (isSocialAuthIntent) {
                handleSocialProviderAuthentication(session)
            }
            if (!isSocialAuthHandled) {
                handleAuthenticatedUser()
            }
        } else {
            navigateToWelcomeIfNeeded()
        }
    }

    private fun processDeepLinks(intent: Intent?): Boolean {
        intent?.data?.let { uri ->
            if (uri.scheme == "https" && uri.host == "excito.netlify.app" && uri.path?.startsWith("/@") == true) {
                val segments = uri.pathSegments
                if (segments.size >= 3 && segments[1] == "event") {
                    val bundle = Bundle().apply {
                        putString("eventId", segments[2])
                        putString("username", segments[0].removePrefix("@"))
                    }
                    navController.navigate(R.id.EventDetailFragment, bundle)
                    return false
                }
            } else if (uri.scheme == "com.events" && uri.host == "login-callback") {
                supabaseClient.handleDeeplinks(intent)
                return true
            }
        }
        return false
    }

    private fun handleAuthenticatedUser() {
        supabaseClient.auth.currentSessionOrNull()?.user?.id?.let { profileViewModel.loadUser(it) }
        navController.navigate(R.id.MainMenuFragment)
    }

    private suspend fun handleSocialProviderAuthentication(session: UserSession) {
        if (session.user == null) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val lastIdentity = session.user?.identities?.maxByOrNull {
            try {
                val lastSignInAt = it.lastSignInAt
                if (lastSignInAt != null) {
                    dateFormat.parse(lastSignInAt)?.time ?: 0L
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
        }
        val lastProvider = lastIdentity?.provider

        when (lastProvider?.lowercase()) {
            "twitter" -> {
                twitterAuthManager.handleTwitterSignInResult(navController)
                isSocialAuthHandled = true
            }
            "facebook" -> {
                facebookAuthManager.handleFacebookSignInResult(navController)
                isSocialAuthHandled = true
            }
            else -> handleAuthenticatedUser()
        }
    }

    private fun navigateToWelcomeIfNeeded() {
        if (navController.currentDestination?.id !in setOf(R.id.WelcomeFragment, R.id.RegistrationFragment)) {
            navController.navigate(R.id.WelcomeFragment)
        }
    }
}