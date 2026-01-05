package com.example.moodpress.feature.user.presentation.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moodpress.MainActivity
import com.example.moodpress.databinding.ActivityUsernameBinding
import com.example.moodpress.feature.user.presentation.viewmodel.UserUpdateState
import com.example.moodpress.feature.user.presentation.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class UsernameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsernameBinding
    private val viewModel: UserViewModel by viewModels()
    private var selectedBirth: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBirthdayPicker()
        setupListeners()
        observeUpdateState()
    }

    private fun setupListeners() {
        binding.nextButton.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun setupBirthdayPicker() {
        binding.buttonBirthdayPicker.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedBirth = calendar.time
                    val dateString = String.format(
                        Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year
                    )
                    binding.buttonBirthdayPicker.setText(dateString)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun validateAndSubmit() {
        val name = binding.nameEditText.text.toString().trim()
        val checkedGenderId = binding.genderGroup.checkedButtonId
        val birth = selectedBirth

        if (name.isEmpty()) {
            showToast("Vui lòng nhập tên.")
            return
        }

        if (checkedGenderId == -1) {
            showToast("Vui lòng chọn giới tính.")
            return
        }

        if (birth == null) {
            showToast("Vui lòng chọn ngày sinh.")
            return
        }

        // Lấy text từ RadioButton đã chọn thông qua ID
        val gender = binding.root.findViewById<RadioButton>(checkedGenderId).text.toString()

        viewModel.saveProfile(name, gender, birth)
    }

    private fun observeUpdateState() {
        lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                binding.nextButton.isEnabled = state !is UserUpdateState.Loading

                when (state) {
                    is UserUpdateState.Loading -> {
                        // Show loading indicator here if needed
                    }
                    is UserUpdateState.Success -> {
                        showToast("Chào mừng, ${state.userName}!")
                        navigateToMain()
                    }
                    is UserUpdateState.Error -> {
                        showToast(state.message)
                    }
                    is UserUpdateState.Idle -> {}
                }
            }
        }
    }

    private fun navigateToMain() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}