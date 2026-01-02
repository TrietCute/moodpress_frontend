package com.example.moodpress.feature.settings.presentation.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.moodpress.R
import com.example.moodpress.core.utils.GoogleAuthManager
import com.example.moodpress.core.utils.NotificationScheduler
import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.databinding.FragmentSettingsBinding
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.feature.settings.presentation.viewmodel.LinkAccountState
import com.example.moodpress.feature.settings.presentation.viewmodel.SettingsViewModel
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var googleAuthManager: GoogleAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    private val viewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveAndScheduleNotification(true)
        } else {
            binding.switchNotification.isChecked = false
            showToast("Cần cấp quyền để nhận thông báo")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupNotificationUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val name = sessionManager.fetchUserName() ?: "Bạn"
        binding.textGreeting.text = "Xin chào, $name!"
    }

    private fun setupClickListeners() {
        with(binding) {
            buttonEditProfile.setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
            }

            buttonLinkAccount.setOnClickListener {
                performGoogleLink()
            }
        }
    }

    private fun performGoogleLink() {
        lifecycleScope.launch {
            val idToken = googleAuthManager.signIn()
            if (idToken != null) {
                viewModel.linkGoogleAccount(idToken)
            } else {
                showToast("Hủy đăng nhập hoặc lỗi")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.linkState.collect { state ->
                    handleLinkState(state)
                }
            }
        }
    }

    private fun handleLinkState(state: LinkAccountState) {
        when (state) {
            is LinkAccountState.Loading -> {
                binding.buttonLinkAccount.isEnabled = false
                showToast("Đang liên kết...")
            }
            is LinkAccountState.Success -> {
                binding.buttonLinkAccount.isEnabled = true
                showToast("Liên kết thành công! Dữ liệu của bạn đã an toàn.")
                showGoogleInfo(state.profile)
            }
            is LinkAccountState.Linked -> {
                showGoogleInfo(state.profile)
            }
            is LinkAccountState.Error -> {
                binding.buttonLinkAccount.isEnabled = true
                showToast(state.message)
            }
            is LinkAccountState.Idle -> {
                binding.cardGoogleInfo.visibility = View.GONE
                binding.buttonLinkAccount.visibility = View.VISIBLE
            }

            else -> {}
        }
    }

    private fun showGoogleInfo(profile: UserProfile) {
        with(binding) {
            cardGoogleInfo.visibility = View.VISIBLE
            tvGoogleName.text = profile.name ?: "Người dùng"
            tvGoogleEmail.text = profile.email ?: ""

            if (!profile.picture.isNullOrEmpty()) {
                Glide.with(this@SettingsFragment)
                    .load(profile.picture)
                    .placeholder(R.drawable.ic_emotion_satisfied)
                    .error(R.drawable.ic_emotion_satisfied)
                    .into(imgGoogleAvatar)
            }
        }
    }

    private fun setupNotificationUI() {
        val isEnabled = sessionManager.isNotificationEnabled()
        val (hour, minute) = sessionManager.getNotificationTime()

        with(binding) {
            switchNotification.isChecked = isEnabled
            updateTimeText(hour, minute)
            updateTimePickerState(isEnabled)

            switchNotification.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkPermissionAndEnable()
                } else {
                    saveAndScheduleNotification(false)
                }
                updateTimePickerState(isChecked)
            }

            layoutTimePicker.setOnClickListener {
                showTimePicker()
            }
        }
    }

    private fun updateTimePickerState(isEnabled: Boolean) {
        binding.layoutTimePicker.alpha = if (isEnabled) 1.0f else 0.5f
        binding.layoutTimePicker.isEnabled = isEnabled
    }

    private fun checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                saveAndScheduleNotification(true)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            saveAndScheduleNotification(true)
        }
    }

    private fun saveAndScheduleNotification(enable: Boolean) {
        val (hour, minute) = sessionManager.getNotificationTime()
        sessionManager.saveNotificationSettings(enable, hour, minute)

        if (enable) {
            notificationScheduler.scheduleReminder(hour, minute)
        } else {
            notificationScheduler.cancelReminder()
        }
    }

    private fun showTimePicker() {
        val (currentHour, currentMinute) = sessionManager.getNotificationTime()

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Chọn giờ nhắc nhở")
            .build()

        picker.addOnPositiveButtonClickListener {
            val newHour = picker.hour
            val newMinute = picker.minute

            updateTimeText(newHour, newMinute)

            val isEnabled = binding.switchNotification.isChecked
            sessionManager.saveNotificationSettings(isEnabled, newHour, newMinute)

            if (isEnabled) {
                notificationScheduler.cancelReminder()
                notificationScheduler.scheduleReminder(newHour, newMinute)
                showToast("Đã cập nhật giờ nhắc: ${String.format("%02d:%02d", newHour, newMinute)}")
            }
        }

        picker.show(childFragmentManager, "tag_time_picker")
    }

    private fun updateTimeText(hour: Int, minute: Int) {
        binding.tvNotificationTime.text = String.format("%02d:%02d", hour, minute)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}