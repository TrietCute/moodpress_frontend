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
import java.util.concurrent.CopyOnWriteArraySet

class SoundMixerService : Service() {

    private val binder = LocalBinder()
    var isPaused = false
    private val listeners = CopyOnWriteArraySet<(Boolean, Boolean) -> Unit>()
    private val activePlayers = HashMap<String, MediaPlayer>()
    private val loadingSounds = HashSet<String>()
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

    fun addListener(listener: (Boolean, Boolean) -> Unit) {
        listeners.add(listener)
        listener(isPlayingAny(), isPaused)
    }

    fun removeListener(listener: (Boolean, Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyStateChange() {
        val isPlaying = isPlayingAny()
        listeners.forEach { it.invoke(isPlaying, isPaused) }
    }

    fun isPlayingAny(): Boolean = activePlayers.isNotEmpty() || loadingSounds.isNotEmpty()

    fun toggleSound(sound: RelaxSound) {
        if (isPaused) resumeAll()

        if (activePlayers.containsKey(sound.audioUrl)) {
            stopPlayer(sound.audioUrl)
        } else if (loadingSounds.contains(sound.audioUrl)) {
            return
        } else {
            handlePlayRequest(sound)
        }
    }

    private fun handlePlayRequest(sound: RelaxSound) {
        val fileName = "sound_${sound.id}.mp3"
        val file = File(cacheDir, fileName)

        if (file.exists() && file.length() > 0) {
            startMediaPlayer(file.absolutePath, sound)
        } else {
            loadingSounds.add(sound.audioUrl)
            notifyStateChange()

            serviceScope.launch {
                val downloadedPath = downloadFile(sound.audioUrl, fileName)
                loadingSounds.remove(sound.audioUrl)
                if (downloadedPath != null) {
                    startMediaPlayer(downloadedPath, sound)
                } else {
                    notifyStateChange()
                }
            }
        }
    }

    fun getActiveUrls(): Set<String> {
        val combinedSet = HashSet<String>()
        combinedSet.addAll(activePlayers.keys)
        combinedSet.addAll(loadingSounds)
        return combinedSet
    }

    private suspend fun downloadFile(urlStr: String, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = File(cacheDir, "${fileName}.tmp")
                val finalFile = File(cacheDir, fileName)

                URL(urlStr).openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.renameTo(finalFile)
                    return@withContext finalFile.absolutePath
                }
                return@withContext null
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun startMediaPlayer(path: String, sound: RelaxSound) {
        try {
            if (activePlayers.containsKey(sound.audioUrl)) return

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
                start()
            }

            activePlayers[sound.audioUrl] = player

            updateNotification()
            notifyStateChange()

        } catch (e: Exception) {
            loadingSounds.remove(sound.audioUrl)
            notifyStateChange()
        }
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

    private fun stopPlayer(url: String) {
        activePlayers[url]?.let { player ->
            if (player.isPlaying) player.stop()
            player.release()
            activePlayers.remove(url)
        }

        if (activePlayers.isEmpty() && loadingSounds.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isPaused = false
        }
        notifyStateChange()
    }

    fun stopAll() {
        activePlayers.values.forEach {
            if (it.isPlaying) it.stop()
            it.release()
        }
        activePlayers.clear()
        loadingSounds.clear()
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
        val contentText = if (activePlayers.isNotEmpty()) {
            "Đang phát ${activePlayers.size} âm thanh thiên nhiên"
        } else {
            "Chạm để quay lại"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MoodPress Relax")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_music_note)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        if (activePlayers.isNotEmpty()) {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopAll()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopAll()
    }
}