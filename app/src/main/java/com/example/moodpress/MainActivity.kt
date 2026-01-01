package com.example.moodpress

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moodpress.databinding.ActivityMainBinding
import com.example.moodpress.feature.relax.service.SoundMixerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var loadingDialog: AlertDialog? = null

    private var soundService: SoundMixerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundMixerService.LocalBinder
            soundService = binder.getService()
            isBound = true

            // 1. Setup Callback: Khi Service báo thay đổi -> Cập nhật UI
            soundService?.onStateChanged = { isPlaying ->
                runOnUiThread {
                    showMiniPlayer(isPlaying)
                }
            }

            // 2. Kiểm tra trạng thái ban đầu (ví dụ: mở lại app khi nhạc đang chạy)
            showMiniPlayer(soundService?.isPlayingAny() == true)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavView.setupWithNavController(navController)

        Intent(this, SoundMixerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        setupMiniPlayer()
    }

    private fun setupMiniPlayer() {
        binding.layoutMiniPlayer.btnStopAll.setOnClickListener {
            soundService?.stopAll()
        }
    }

    fun showMiniPlayer(show: Boolean) {
        val miniPlayer = binding.layoutMiniPlayer.root
        if (show) {
            if (miniPlayer.visibility != View.VISIBLE) {
                miniPlayer.visibility = View.VISIBLE
                miniPlayer.alpha = 0f
                miniPlayer.animate().alpha(1f).duration = 300
            }
        } else {
            if (miniPlayer.visibility != View.GONE) {
                miniPlayer.animate().alpha(0f).withEndAction {
                    miniPlayer.visibility = View.GONE
                }.duration = 300
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            soundService?.onStateChanged = null
            unbindService(connection)
            isBound = false
        }
    }

    fun showLoading(message: String) {
        hideLoading()

        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    fun hideLoading() {
        if (loadingDialog != null && loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
        loadingDialog = null
    }
}