package com.komiker.events.ui.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView
    private val supabaseClient = SupabaseClientProvider.client
    private val minTimeBeforeExpiry = 1800L
    private val splashDuration = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeView()
        setupSystemBars()
        startSplashFlow()
    }

    private fun initializeView() {
        setContentView(R.layout.splash_screen)
        lottieAnimationView = findViewById(R.id.lottie_animation_view)
    }

    private fun setupSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.neutral_100)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.neutral_100)
    }

    private fun startSplashFlow() {
        lifecycleScope.launch(Dispatchers.Main) {
            val isAuthenticated = withContext(Dispatchers.IO) { checkAuthentication() }
            delay(splashDuration)

            val options = ActivityOptions.makeCustomAnimation(this@SplashActivity, 0, 0)
            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                putExtra("isAuthenticated", isAuthenticated)
            }
            startActivity(intent, options.toBundle())
            finish()
        }
    }

    private suspend fun checkAuthentication(): Boolean {
        val session = supabaseClient.auth.currentSessionOrNull() ?: return false
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

    private fun shouldRefreshToken(session: UserSession): Boolean {
        val currentTime = Clock.System.now()
        return (session.expiresAt - currentTime).inWholeSeconds < minTimeBeforeExpiry
    }
}
