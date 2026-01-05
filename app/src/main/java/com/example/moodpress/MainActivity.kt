package com.example.moodpress

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moodpress.databinding.ActivityMainBinding
import com.example.moodpress.feature.relax.presentation.viewmodel.RelaxViewModel
import com.example.moodpress.feature.relax.service.SoundMixerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val relaxViewModel: RelaxViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var loadingDialog: AlertDialog? = null
    var soundService: SoundMixerService? = null
    private var isBound = false
    private var diskAnimator: ObjectAnimator? = null

    private var dX = 0f
    private var dY = 0f
    private var lastAction = 0
    private var clickStartTime = 0L

    private val stateListener: (Boolean, Boolean) -> Unit = { _, _ ->
        runOnUiThread { updateMiniPlayerState() }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundMixerService.LocalBinder
            soundService = binder.getService()
            isBound = true

            soundService?.addListener(stateListener)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            soundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        setupMiniPlayerControls()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SoundMixerService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            soundService?.removeListener(stateListener)
            unbindService(connection)
            isBound = false
        }
        diskAnimator?.cancel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        binding.bottomNavView.setupWithNavController(navHostFragment.navController)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMiniPlayerControls() {
        diskAnimator = ObjectAnimator.ofFloat(binding.miniPlayer.imgDisk, "rotation", 0f, 360f).apply {
            duration = 10000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        binding.miniPlayer.apply {
            btnPauseResume.setOnClickListener {
                if (isBound && soundService != null) {
                    if (soundService!!.isPaused) soundService?.resumeAll() else soundService?.pauseAll()
                }
            }
            btnStop.setOnClickListener {
                if (isBound) soundService?.stopAll()
            }
            root.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                        clickStartTime = System.currentTimeMillis()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (layoutControls.isGone) {
                            view.animate()
                                .x(event.rawX + dX)
                                .y(event.rawY + dY)
                                .setDuration(0)
                                .start()
                            lastAction = MotionEvent.ACTION_MOVE
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - clickStartTime
                        if (duration < 200 && lastAction == MotionEvent.ACTION_DOWN) {
                            layoutControls.isVisible = !layoutControls.isVisible
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun updateMiniPlayerState() {
        val service = soundService ?: return
        val isPlayingAny = service.isPlayingAny()
        val isPaused = service.isPaused

        if (isPlayingAny) {
            if (!binding.miniPlayer.root.isVisible) {
                binding.miniPlayer.root.translationX = 0f
                binding.miniPlayer.root.translationY = 0f
                binding.miniPlayer.root.isVisible = true
            }

            binding.miniPlayer.apply {
                val iconRes = if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
                btnPauseResume.setImageResource(iconRes)
                if (layoutControls.isGone) {
                    layoutControls.isVisible = true
                }
            }
            handleDiskAnimation(true, isPaused)
        } else {
            binding.miniPlayer.root.isVisible = false
            handleDiskAnimation(false, false)
        }
    }

    private fun handleDiskAnimation(isPlayingAny: Boolean, isPaused: Boolean) {
        val animator = diskAnimator ?: return
        if (isPlayingAny && !isPaused) {
            if (!animator.isStarted) animator.start() else if (animator.isPaused) animator.resume()
        } else if (isPlayingAny && isPaused) {
            if (animator.isRunning) animator.pause()
        } else {
            if (animator.isRunning) animator.cancel()
        }
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