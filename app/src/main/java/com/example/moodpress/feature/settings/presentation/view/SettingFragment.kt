package com.example.moodpress.feature.settings.presentation.view

import android.Manifest
import android.app.TimePickerDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.moodpress.R
import com.example.moodpress.core.utils.GoogleAuthManager
import com.example.moodpress.core.utils.NotificationScheduler
import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.databinding.FragmentSettingsBinding
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.features.settings.presentation.viewmodel.LinkAccountState
import com.example.moodpress.features.settings.presentation.viewmodel.SettingsViewModel
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Quyền được cấp -> Bật switch và lưu
            saveAndScheduleNotification(true)
        } else {
            // Quyền bị từ chối -> Tắt switch
            binding.switchNotification.isChecked = false
            Toast.makeText(context, "Cần cấp quyền để nhận thông báo", Toast.LENGTH_SHORT).show()
        }
    }

    private val viewModel: SettingsViewModel by viewModels()

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
        // 1. Nút Chỉnh sửa thông tin
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        // 2. Nút Liên kết tài khoản
        binding.buttonLinkAccount.setOnClickListener {
            performGoogleLink()
        }
    }

    private fun performGoogleLink() {
        lifecycleScope.launch {
            // Gọi hộp thoại Google Sign-In
            val idToken = googleAuthManager.signIn()

            if (idToken != null) {
                // Nếu lấy được token, gọi ViewModel để gửi lên server
                viewModel.linkGoogleAccount(idToken)
            } else {
                Toast.makeText(context, "Hủy đăng nhập hoặc lỗi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.linkState.collect { state ->
                when (state) {
                    is LinkAccountState.Loading -> {
                        // Bạn có thể hiện ProgressBar ở đây
                        // binding.progressBar.isVisible = true
                        binding.buttonLinkAccount.isEnabled = false
                        Toast.makeText(context, "Đang liên kết...", Toast.LENGTH_SHORT).show()
                    }
                    is LinkAccountState.Success -> {
                        binding.buttonLinkAccount.isEnabled = true

                        Toast.makeText(context, "Liên kết thành công! Dữ liệu của bạn đã an toàn.", Toast.LENGTH_LONG).show()

                        showGoogleInfo(state.profile)
                    }
                    is LinkAccountState.Linked -> {
                        showGoogleInfo(state.profile)
                    }
                    is LinkAccountState.Error -> {
                        binding.buttonLinkAccount.isEnabled = true
                        binding.buttonLinkAccount.alpha = 1.0f
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    is LinkAccountState.Idle -> {
                        binding.cardGoogleInfo.visibility = View.GONE
                        binding.buttonLinkAccount.visibility = View.VISIBLE
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showGoogleInfo(profile: UserProfile) {
        binding.cardGoogleInfo.visibility = View.VISIBLE

        binding.tvGoogleName.text = profile.name ?: "Người dùng"
        binding.tvGoogleEmail.text = profile.email ?: ""

        if (!profile.picture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profile.picture)
                .placeholder(R.drawable.ic_emotion_satisfied)
                .error(R.drawable.ic_emotion_satisfied)
                .into(binding.imgGoogleAvatar)
        }
    }

    private fun setupNotificationUI() {
        val isEnabled = sessionManager.isNotificationEnabled()
        val (hour, minute) = sessionManager.getNotificationTime()

        binding.switchNotification.isChecked = isEnabled
        updateTimeText(hour, minute)

        binding.layoutTimePicker.alpha = if (isEnabled) 1.0f else 0.5f
        binding.layoutTimePicker.isEnabled = isEnabled

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionAndEnable()
            } else {
                saveAndScheduleNotification(false)
            }

            binding.layoutTimePicker.alpha = if (isChecked) 1.0f else 0.5f
            binding.layoutTimePicker.isEnabled = isChecked
        }

        binding.layoutTimePicker.setOnClickListener {
            showTimePicker()
        }
    }

    private fun checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
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

        // 1. Tạo MaterialTimePicker
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
                Toast.makeText(context, "Đã cập nhật giờ nhắc: ${String.format("%02d:%02d", newHour, newMinute)}", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Hiển thị (Dùng childFragmentManager)
        picker.show(childFragmentManager, "tag_time_picker")
    }

    private fun updateTimeText(hour: Int, minute: Int) {
        val timeString = String.format("%02d:%02d", hour, minute)
        binding.tvNotificationTime.text = timeString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}