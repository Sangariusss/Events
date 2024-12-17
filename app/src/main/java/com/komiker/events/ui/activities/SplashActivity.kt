package com.komiker.events.ui.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.komiker.events.R
import com.komiker.events.data.database.SupabaseClientProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView
    private val splashDuration = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        SupabaseClientProvider.client
        window.statusBarColor = ContextCompat.getColor(this, R.color.neutral_100)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.neutral_100)

        lottieAnimationView = findViewById(R.id.lottie_animation_view)

        CoroutineScope(Dispatchers.Main).launch {
            delay(splashDuration)

            val options = ActivityOptions.makeCustomAnimation(
                this@SplashActivity,
                0,
                0
            )

            startActivity(Intent(this@SplashActivity, MainActivity::class.java), options.toBundle())
            finish()
        }
    }
}