package com.example.moodpress.feature.user.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentEditProfileBinding
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.feature.user.presentation.viewmodel.EditProfileViewModel
import com.example.moodpress.feature.user.presentation.viewmodel.ProfileUiState
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    private var selectedBirthday: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // 1. Nút Quay lại
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 2. Chọn ngày sinh
        binding.buttonBirthdayPicker.setOnClickListener {
            showBirthdayPicker()
        }

        // 3. Lưu thông tin
        binding.buttonSave.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val gender = getSelectedGender()

            viewModel.saveProfile(name, gender, selectedBirthday)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Ẩn hiện loading nếu cần

                when (state) {
                    is ProfileUiState.Loading -> {
                        binding.buttonSave.isEnabled = false
                        binding.buttonSave.text = "Đang lưu..."
                    }
                    is ProfileUiState.Success -> {
                        Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is ProfileUiState.Error -> {
                        binding.buttonSave.isEnabled = true
                        binding.buttonSave.text = "Lưu thay đổi"
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is ProfileUiState.Loaded -> {
                        // Điền dữ liệu cũ vào Form
                        binding.buttonSave.isEnabled = true
                        binding.buttonSave.text = "Lưu thay đổi"
                        fillDataToUI(state.profile)
                    }
                }
            }
        }
    }

    private fun fillDataToUI(profile: UserProfile) {
        // Tên
        binding.nameEditText.setText(profile.name)

        // Ngày sinh
        profile.birth?.let {
            selectedBirthday = it
            updateBirthdayText(it)
        }

        // Giới tính
        when (profile.gender) {
            "Nam" -> binding.radioMale.isChecked = true
            "Nữ" -> binding.radioFemale.isChecked = true
            "Khác" -> binding.radioOther.isChecked = true
        }
    }

    private fun getSelectedGender(): String? {
        return when (binding.genderGroup.checkedRadioButtonId) {
            R.id.radio_male -> "Nam"
            R.id.radio_female -> "Nữ"
            R.id.radio_other -> "Khác"
            else -> null
        }
    }

    private fun showBirthdayPicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày sinh")
            .setSelection(selectedBirthday?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedBirthday = Date(selection)
            updateBirthdayText(selectedBirthday!!)
        }
        datePicker.show(childFragmentManager, "BIRTHDAY_PICKER")
    }

    private fun updateBirthdayText(date: Date) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        binding.birthdayEditText.setText(sdf.format(date))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}