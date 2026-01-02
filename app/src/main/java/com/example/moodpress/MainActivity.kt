package com.example.moodpress

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moodpress.core.utils.DraggableTouchListener
import com.example.moodpress.databinding.ActivityMainBinding
import com.example.moodpress.feature.relax.presentation.viewmodel.RelaxViewModel
import com.example.moodpress.feature.relax.service.SoundMixerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val relaxViewModel: RelaxViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var loadingDialog: AlertDialog? = null

    private var soundService: SoundMixerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundMixerService.LocalBinder
            soundService = binder.getService()
            isBound = true

            soundService?.onStateChanged = { isPlaying, isPaused ->
                handleServiceStateChange(isPlaying, isPaused)
            }

            // Sync initial state
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

        setupNavigation()
        setupMiniPlayer()

        Intent(this, SoundMixerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
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

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        binding.bottomNavView.setupWithNavController(navHostFragment.navController)
    }

    private fun handleServiceStateChange(isPlaying: Boolean, isPaused: Boolean) {
        if (!isPlaying) {
            showMiniPlayer(false)
            relaxViewModel.resetAllSoundsUI()
            return
        }

        showMiniPlayer(true)
        with(binding.layoutMiniPlayer) {
            val iconRes = if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
            btnPauseResume.setImageResource(iconRes)

            if (isPaused) {
                imgDisk.clearAnimation()
            } else {
                startDiskAnimation(imgDisk)
            }
        }
    }

    private fun setupMiniPlayer() {
        with(binding.layoutMiniPlayer) {
            root.setOnTouchListener(DraggableTouchListener {
                toggleControlMenu()
            })

            btnPauseResume.setOnClickListener {
                if (soundService?.isPaused == true) {
                    soundService?.resumeAll()
                } else {
                    soundService?.pauseAll()
                }
            }

            btnStop.setOnClickListener {
                soundService?.stopAll()
            }
        }
    }

    private fun toggleControlMenu() {
        val controls = binding.layoutMiniPlayer.layoutControls
        val isVisible = controls.visibility == View.VISIBLE

        if (isVisible) {
            controls.visibility = View.GONE
        } else {
            controls.visibility = View.VISIBLE
            // Auto hide controls after 5 seconds
            controls.postDelayed({ controls.visibility = View.GONE }, 5000)
        }
    }

    fun showMiniPlayer(show: Boolean) {
        val miniPlayer = binding.layoutMiniPlayer.root
        val imgDisk = binding.layoutMiniPlayer.imgDisk

        if (show) {
            if (miniPlayer.visibility != View.VISIBLE) {
                miniPlayer.visibility = View.VISIBLE
                miniPlayer.alpha = 0f
                miniPlayer.scaleX = 0f
                miniPlayer.scaleY = 0f

                miniPlayer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()

                startDiskAnimation(imgDisk)
            }
        } else {
            if (miniPlayer.visibility != View.GONE) {
                miniPlayer.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(300)
                    .withEndAction {
                        miniPlayer.visibility = View.GONE
                        imgDisk.clearAnimation()
                    }
                    .start()
            }
        }
    }

    private fun startDiskAnimation(view: View) {
        // Avoid restarting animation if already running
        if (view.animation != null && view.animation.hasStarted() && !view.animation.hasEnded()) return

        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 5000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        view.startAnimation(rotate)
    }

    // --- Loading Dialog Helpers ---

    fun showLoading(message: String) {
        hideLoading()
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    fun hideLoading() {
        if (loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
        loadingDialog = null
    }
}