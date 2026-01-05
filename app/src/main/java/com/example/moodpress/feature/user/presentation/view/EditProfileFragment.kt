package com.example.moodpress.feature.user.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            birthdayEditText.setOnClickListener {
                showBirthdayPicker()
            }

            buttonSave.setOnClickListener {
                validateAndSave()
            }
        }
    }

    private fun validateAndSave() {
        val name = binding.nameEditText.text.toString().trim()

        if (name.isEmpty()) {
            showToast("Vui lòng nhập tên")
            return
        }

        val gender = getSelectedGender()
        viewModel.saveProfile(name, gender, selectedBirthday)
    }

    private fun observeViewModel() {
        // Sử dụng viewLifecycleOwner để đảm bảo flow hủy theo view của fragment
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: ProfileUiState) {
        val isButtonEnabled = state !is ProfileUiState.Loading
        binding.buttonSave.isEnabled = isButtonEnabled
        binding.buttonSave.text = if (state is ProfileUiState.Loading) "Đang lưu..." else "Lưu thay đổi"

        when (state) {
            is ProfileUiState.Loading -> { /* Handled above */ }
            is ProfileUiState.Success -> {
                showToast("Cập nhật thành công!")
                findNavController().popBackStack()
            }
            is ProfileUiState.Error -> {
                showToast(state.message)
            }
            is ProfileUiState.Loaded -> {
                fillDataToUI(state.profile)
            }
        }
    }

    private fun fillDataToUI(profile: UserProfile) {
        binding.nameEditText.setText(profile.name)

        profile.birth?.let {
            selectedBirthday = it
            updateBirthdayText(it)
        }

        when (profile.gender) {
            "Nam" -> binding.btnGenderMale.isChecked = true
            "Nữ" -> binding.btnGenderFemale.isChecked = true
            "Khác" -> binding.btnOther.isChecked = true
        }
    }

    private fun getSelectedGender(): String? {
        return when (binding.genderToggleGroup.checkedButtonId) {
            R.id.btn_gender_male -> "Nam"
            R.id.btn_gender_female -> "Nữ"
            R.id.btn_other -> "Khác"
            else -> null
        }
    }

    private fun showBirthdayPicker() {
        val selection = selectedBirthday?.time ?: MaterialDatePicker.todayInUtcMilliseconds()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày sinh")
            .setSelection(selection)
            .build()

        datePicker.addOnPositiveButtonClickListener { timestamp ->
            val date = Date(timestamp)
            selectedBirthday = date
            updateBirthdayText(date)
        }
        datePicker.show(childFragmentManager, "BIRTHDAY_PICKER")
    }

    private fun updateBirthdayText(date: Date) {
        // Sử dụng UTC để khớp với MaterialDatePicker selection
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        binding.birthdayEditText.setText(sdf.format(date))
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}