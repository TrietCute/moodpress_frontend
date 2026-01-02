package com.example.moodpress.feature.relax.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.moodpress.R
import com.example.moodpress.feature.relax.domain.model.RelaxSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class SoundMixerService : Service() {

    private val binder = LocalBinder()
    var isPaused = false
    var onStateChanged: ((Boolean, Boolean) -> Unit)? = null
    val activePlayers = HashMap<String, MediaPlayer>()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val CHANNEL_ID = "RelaxSoundChannel"
        const val NOTIFICATION_ID = 123
    }

    inner class LocalBinder : Binder() {
        fun getService(): SoundMixerService = this@SoundMixerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun isPlayingAny(): Boolean = activePlayers.isNotEmpty()

    fun toggleSound(sound: RelaxSound) {
        if (isPaused) resumeAll()
        if (activePlayers.containsKey(sound.audioUrl)) {
            stopPlayer(sound.audioUrl)
            sound.isPlaying = false
        } else {
            playPlayer(sound)
            sound.isPlaying = true
        }
        updateNotification()
        notifyStateChange()
    }

    private fun notifyStateChange() {
        onStateChanged?.invoke(activePlayers.isNotEmpty(), isPaused)
    }

    fun pauseAll() {
        if (activePlayers.isNotEmpty() && !isPaused) {
            activePlayers.values.forEach { if (it.isPlaying) it.pause() }
            isPaused = true
            updateNotification()
            notifyStateChange()
        }
    }

    fun resumeAll() {
        if (activePlayers.isNotEmpty() && isPaused) {
            activePlayers.values.forEach { it.start() }
            isPaused = false
            updateNotification()
            notifyStateChange()
        }
    }

    private suspend fun downloadFile(context: Context, url: String, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, fileName)

                if (file.exists() && file.length() > 0) {
                    return@withContext file.absolutePath
                }

                URL(url).openStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun playPlayer(sound: RelaxSound) {
        Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()

        serviceScope.launch {
            val fileName = "sound_${sound.id}.mp3"
            val localPath = downloadFile(applicationContext, sound.audioUrl, fileName)

            if (localPath != null) {
                startMediaPlayer(localPath, sound)
            } else {
                Toast.makeText(applicationContext, "Lỗi tải nhạc. Kiểm tra kết nối!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMediaPlayer(path: String, sound: RelaxSound) {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)

                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
            }
            player.start()
            activePlayers[sound.audioUrl] = player

            updateNotification()
            notifyStateChange()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopPlayer(url: String) {
        activePlayers[url]?.let { player ->
            if (player.isPlaying) player.stop()
            player.release()
            activePlayers.remove(url)
        }

        if (activePlayers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            //stopSelf()
        }
        notifyStateChange()
    }

    fun stopAll() {
        activePlayers.values.forEach {
            if (it.isPlaying) it.stop()
            it.release()
        }
        activePlayers.clear()
        isPaused = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        notifyStateChange()
    }

    fun isPlaying(url: String): Boolean {
        return activePlayers.containsKey(url)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MoodPress Relax Sounds",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MoodPress Relax")
            .setContentText("Đang phát âm thanh thư giãn...")
            .setSmallIcon(R.drawable.ic_music_note)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        if (activePlayers.isNotEmpty()) {
            val notification = buildNotification()
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}