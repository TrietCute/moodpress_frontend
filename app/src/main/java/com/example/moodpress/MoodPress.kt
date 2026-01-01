package com.example.moodpress

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.cloudinary.android.MediaManager
import com.example.moodpress.BuildConfig

@HiltAndroidApp
class MoodPress() : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Khởi tạo Cloudinary
        val config = HashMap<String, String>()
        config["cloud_name"] = BuildConfig.CLOUD_NAME
        config["api_key"] = BuildConfig.CLOUD_API_KEY
        config["secure"] = "true"

        android.util.Log.d("CloudinaryCheck", "Config: $config")
        MediaManager.init(this, config)
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "moodpress_reminder_channel"
            val name = "Nhắc nhở viết nhật ký"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH

            val channel = android.app.NotificationChannel(channelId, name, importance)
            channel.description = "Thông báo nhắc nhở hàng ngày"

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}