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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)
    private val facebookAuthManager = FacebookAuthManager()
    private val twitterAuthManager = TwitterAuthManager()
    private val profileViewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(supabaseUserDao) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBindingAndNavigation()
        lifecycleScope.launch { handleInitialFlow() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch { processDeepLinks(intent) }
    }

    private fun initializeBindingAndNavigation() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_nav_host_content_main) as NavHostFragment
        navController = navHostFragment.navController
    }

    private suspend fun handleInitialFlow() {
        val isAuthenticated = intent.getBooleanExtra("isAuthenticated", false)

        if (isAuthenticated) {
            handleSocialProviderAuthentication()
            handleAuthenticatedUser()
        } else {
            navigateToWelcomeIfNeeded()
        }

        processDeepLinks(intent)
    }

    private fun processDeepLinks(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "https" && uri.host == "excito.netlify.app" && uri.path?.startsWith("/@") == true) {
                val segments = uri.pathSegments
                if (segments.size >= 3 && segments[1] == "event") {
                    val bundle = Bundle().apply {
                        putString("eventId", segments[2])
                        putString("username", segments[0].removePrefix("@"))
                    }
                    navController.navigate(R.id.EventDetailFragment, bundle)
                }
            } else {
                supabaseClient.handleDeeplinks(intent)
            }
        }
    }

    private fun handleAuthenticatedUser() {
        supabaseClient.auth.currentSessionOrNull()?.user?.id?.let { profileViewModel.loadUser(it) }
        navController.navigate(R.id.MainMenuFragment)
    }

    private suspend fun handleSocialProviderAuthentication() {
        val session = supabaseClient.auth.currentSessionOrNull() ?: return

        val provider = session.accessToken.let { token ->
            when {
                token.contains("twitter", ignoreCase = true) -> "twitter"
                token.contains("facebook", ignoreCase = true) -> "facebook"
                else -> "unknown"
            }
        }

        when (provider) {
            "twitter" -> twitterAuthManager.handleTwitterSignInResult(navController)
            "facebook" -> facebookAuthManager.handleFacebookSignInResult(navController)
        }
    }

    private fun navigateToWelcomeIfNeeded() {
        if (navController.currentDestination?.id !in setOf(R.id.WelcomeFragment, R.id.RegistrationFragment)) {
            navController.navigate(R.id.WelcomeFragment)
        }
    }
}