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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.UUID
import com.example.moodpress.databinding.ActivityUsernameBinding
import com.example.moodpress.feature.user.presentation.view.UsernameActivity

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint // Báo cho Hilt biết để tiêm (inject) SessionManager
public final class SplashActivity : AppCompatActivity() {

    @Inject // Yêu cầu Hilt tiêm SessionManager
    lateinit var sessionManager: SessionManager

    private val SPLASH_DELAY: Long = 2000 // Chờ 2 giây

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set layout đã tạo

        setupUserSession()
        // Chạy logic kiểm tra User IDc
        val nextActivityClass = decideNextActivity()

        // 4. Bắt đầu đếm giờ
        Handler(Looper.getMainLooper()).postDelayed({
            // 5. Sau 2 giây, chạy hàm chuyển màn hình
            navigateTo(nextActivityClass)
        }, SPLASH_DELAY)
    }

    private fun setupUserSession() {
        // 1. Kiểm tra xem User ID đã tồn tại chưa
        val currentUserId = sessionManager.fetchUserId()

        // 2. Nếu chưa tồn tại (lần đầu mở app)
        if (currentUserId == null) {
            // Tạo một User ID mới (UUID)
            val newUserId = UUID.randomUUID().toString()

            // Lưu lại ngay lập tức
            sessionManager.saveUserId(newUserId)
        }
    }

    private fun decideNextActivity(): Class<*> {
        val userName = sessionManager.fetchUserName()

        return if (userName == null) {
            // Nếu CHƯA có tên -> Đi đến màn hình Nhập tên
            UsernameActivity::class.java
        } else {
            // Nếu ĐÃ có tên -> Đi vào màn hình chính
            MainActivity::class.java
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Hủy SplashActivity
    }
}