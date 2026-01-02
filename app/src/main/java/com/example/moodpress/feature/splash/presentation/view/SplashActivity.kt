package com.example.moodpress.feature.splash.presentation.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.moodpress.MainActivity
import com.example.moodpress.R
import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.feature.user.presentation.view.UsernameActivity
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setupUserSession()

        val nextActivityClass = decideNextActivity()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateTo(nextActivityClass)
        }, SPLASH_DELAY)
    }

    private fun setupUserSession() {
        if (sessionManager.fetchUserId() == null) {
            sessionManager.saveUserId(UUID.randomUUID().toString())
        }
    }

    private fun decideNextActivity(): Class<*> {
        val userName = sessionManager.fetchUserName()
        return if (userName == null) {
            UsernameActivity::class.java
        } else {
            MainActivity::class.java
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val SPLASH_DELAY: Long = 2000
    }
}