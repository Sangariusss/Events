package com.komiker.events.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
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
import kotlinx.coroutines.Job
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
    private var startupJob: Job? = null

    companion object {
        private val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private const val SESSION_WAIT_TIMEOUT = 1500L
        private const val SESSION_ATTEMPT_DELAY = 50L
        private const val MAX_SESSION_ATTEMPTS = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBindingAndNavigation()
        setupCustomBackButtonHandler()
        startApplicationFlow(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        startApplicationFlow(intent)
    }

    private fun initializeBindingAndNavigation() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_nav_host_content_main) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun startApplicationFlow(currentIntent: Intent?) {
        startupJob?.cancel()
        startupJob = lifecycleScope.launch { handleAppStartup(currentIntent) }
    }

    private fun setupCustomBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, true) {
            val isAuthenticated = supabaseClient.auth.currentSessionOrNull() != null
            val currentId = navController.currentDestination?.id
            val welcomeScreens = setOf(R.id.WelcomeFragment, R.id.RegistrationFragment)
            val mainMenuId = R.id.MainMenuFragment

            val navOptionsToRoot = navOptions {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }

            if (isAuthenticated) {
                if (currentId != mainMenuId) {
                    navController.navigate(mainMenuId, null, navOptionsToRoot)
                } else {
                    finish()
                }
            } else {
                if (currentId !in welcomeScreens) {
                    navController.navigate(R.id.WelcomeFragment, null, navOptionsToRoot)
                } else {
                    finish()
                }
            }
        }
    }

    private suspend fun handleAppStartup(currentIntent: Intent?) {
        isSocialAuthHandled = false
        val linkType = processDeepLinks(currentIntent)

        val isContentLink = (linkType == 1)
        val isSocialAuthIntent = (linkType == 2)

        val session = withTimeoutOrNull(SESSION_WAIT_TIMEOUT) {
            var currentSession = supabaseClient.auth.currentSessionOrNull()
            var attempts = 0
            while (currentSession == null && (isSocialAuthIntent || attempts < MAX_SESSION_ATTEMPTS)) {
                delay(SESSION_ATTEMPT_DELAY)
                currentSession = supabaseClient.auth.currentSessionOrNull()
                if (!isSocialAuthIntent) attempts++
            }
            currentSession
        } ?: supabaseClient.auth.currentSessionOrNull()

        session?.user?.id?.let { profileViewModel.loadUser(it) }

        if (session?.user != null) {
            if (isSocialAuthIntent) {
                handleSocialProviderAuthentication(session)
            }

            if (!isContentLink && !isSocialAuthHandled) {
                val currentId = navController.currentDestination?.id
                if (currentId != R.id.MainMenuFragment) {
                    navController.navigate(R.id.MainMenuFragment)
                }
            }
        } else {
            if (!isContentLink) {
                navigateToWelcomeIfNeeded()
            }
        }
    }

    private fun processDeepLinks(intentToProcess: Intent?): Int {
        intentToProcess?.data?.let { uri ->
            if (uri.scheme == "https" && uri.host == "excito.netlify.app" && uri.path?.startsWith("/@") == true) {
                val segments = uri.pathSegments
                if (segments.size >= 3 && segments[1] == "event") {
                    val bundle = Bundle().apply {
                        putString("eventId", segments[2])
                        putString("username", segments[0].removePrefix("@"))
                    }
                    if (navController.currentDestination?.id != R.id.EventDetailFragment) {
                        navController.navigate(R.id.EventDetailFragment, bundle)
                    }
                    return 1
                }
            } else if (uri.scheme == "com.events" && uri.host == "login-callback") {
                supabaseClient.handleDeeplinks(intentToProcess)
                return 2
            }
        }
        return 0
    }

    private suspend fun handleSocialProviderAuthentication(session: UserSession) {
        session.user?.identities?.maxByOrNull {
            try {
                it.lastSignInAt?.let { date -> UTC_DATE_FORMAT.parse(date)?.time } ?: 0L
            } catch (e: Exception) {
                0L
            }
        }?.provider?.let { lastProvider ->
            when (lastProvider.lowercase()) {
                "twitter" -> {
                    twitterAuthManager.handleTwitterSignInResult(navController)
                    isSocialAuthHandled = true
                }
                "facebook" -> {
                    facebookAuthManager.handleFacebookSignInResult(navController)
                    isSocialAuthHandled = true
                }
            }
        }
    }

    private fun navigateToWelcomeIfNeeded() {
        val welcomeScreens = setOf(R.id.WelcomeFragment, R.id.RegistrationFragment)
        if (navController.currentDestination?.id !in welcomeScreens) {
            navController.navigate(R.id.WelcomeFragment)
        }
    }
}