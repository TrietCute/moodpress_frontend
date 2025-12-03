package com.example.moodpress.feature.user.presentation.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moodpress.MainActivity
import com.example.moodpress.core.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.moodpress.databinding.ActivityUsernameBinding
import com.example.moodpress.feature.user.presentation.viewmodel.UserViewModel
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.moodpress.feature.user.presentation.viewmodel.UserUpdateState
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
public final class UsernameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsernameBinding
    private val viewModel: UserViewModel by viewModels()
    private var selectedBirth: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo ViewBinding
        binding = ActivityUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBirthdayPicker()
        setupListeners()
        observeUpdateState()
    }

    private fun setupListeners() {
        binding.nextButton.setOnClickListener {
            handleNextClick()
        }
    }

    private fun handleNextClick() {
        val name = binding.nameEditText.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên.", Toast.LENGTH_SHORT).show()
            return
        }

        // Lấy giới tính từ RadioGroup
        val checkedId = binding.genderGroup.checkedRadioButtonId
        if (checkedId == -1) {
            Toast.makeText(this, "Vui lòng chọn giới tính.", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = findViewById<RadioButton>(checkedId).text.toString()

        // Kiểm tra ngày sinh
        val birth = selectedBirth
        if (birth == null) {
            Toast.makeText(this, "Vui lòng chọn ngày sinh.", Toast.LENGTH_SHORT).show()
            return
        }

        // Gửi lên ViewModel
        viewModel.saveProfile(name, gender, birth)
    }

    private fun setupBirthdayPicker() {
        binding.buttonBirthdayPicker.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedBirth = calendar.time

                    // Hiển thị ngày đã chọn lên button
                    binding.buttonBirthdayPicker.text =
                        "$dayOfMonth/${month + 1}/$year"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun observeUpdateState() {
        lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                // Ẩn/hiện nút và ProgressBar
                binding.nextButton.isEnabled = state !is UserUpdateState.Loading
                // (Bạn nên thêm 1 ProgressBar vào file activity_username.xml)
                // binding.progressBar.isVisible = state is UserUpdateState.Loading

                when (state) {
                    is UserUpdateState.Loading -> {
                        // Đang tải...
                    }
                    is UserUpdateState.Success -> {
                        // Thành công! Chuyển màn hình
                        Toast.makeText(this@UsernameActivity, "Chào mừng, ${state.userName}!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    is UserUpdateState.Error -> {
                        // Có lỗi, hiển thị thông báo
                        Toast.makeText(this@UsernameActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is UserUpdateState.Idle -> {
                        // Trạng thái chờ
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Cờ này xóa UsernameActivity khỏi stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}