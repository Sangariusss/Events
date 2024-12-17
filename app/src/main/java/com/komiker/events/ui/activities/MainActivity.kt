package com.komiker.events.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import com.komiker.events.data.database.dao.SupabaseUserDao
import com.komiker.events.databinding.ActivityMainBinding
import com.komiker.events.viewmodels.ProfileViewModel
import com.komiker.events.viewmodels.ProfileViewModelFactory
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val supabaseClient = SupabaseClientProvider.client
    private val supabaseUserDao = SupabaseUserDao(supabaseClient)

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(supabaseUserDao)
    }

    companion object {
        private const val MIN_TIME_BEFORE_EXPIRY = 1800
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supabaseClient.handleDeeplinks(intent)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_nav_host_content_main) as NavHostFragment
        navController = navHostFragment.navController

        lifecycleScope.launch {
            if (isUserAuthenticated()) {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                userId?.let { profileViewModel.loadUser(it) }
                navController.navigate(R.id.MainMenuFragment)
            } else {
                navController.navigate(R.id.WelcomeFragment)
            }
        }
    }

    private suspend fun isUserAuthenticated(): Boolean {
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null) {
            if (shouldRefreshToken(session)) {
                try {
                    supabaseClient.auth.refreshCurrentSession()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }
            return true
        }
        return false
    }

    private fun shouldRefreshToken(session: UserSession): Boolean {
        val currentTime = Clock.System.now()
        val expiryTime = session.expiresAt

        return (expiryTime - currentTime).inWholeSeconds < MIN_TIME_BEFORE_EXPIRY
    }
}