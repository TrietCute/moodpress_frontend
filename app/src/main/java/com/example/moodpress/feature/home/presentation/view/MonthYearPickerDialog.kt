package com.example.moodpress.feature.home.presentation.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.moodpress.databinding.DialogMonthYearPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class MonthYearPickerDialog(
    private val initialYear: Int,
    private val initialMonth: Int, // 0-11 (Giống Calendar)
    private val listener: OnMonthYearSelectedListener
) : DialogFragment() {

    // Interface để gửi dữ liệu về HomeFragment
    interface OnMonthYearSelectedListener {
        fun onMonthYearSelected(year: Int, month: Int) // 0-11
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate layout dùng ViewBinding
        val binding = DialogMonthYearPickerBinding.inflate(layoutInflater)

        val calendar = Calendar.getInstance()

        // --- Cài đặt Cột Tháng ---
        val months = arrayOf(
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        )
        binding.pickerMonth.minValue = 0
        binding.pickerMonth.maxValue = 11
        binding.pickerMonth.displayedValues = months
        binding.pickerMonth.value = initialMonth // Đặt giá trị tháng hiện tại

        // --- Cài đặt Cột Năm ---
        val currentYear = calendar.get(Calendar.YEAR)
        binding.pickerYear.minValue = currentYear - 10 // Cho cuộn 10 năm về trước
        binding.pickerYear.maxValue = currentYear + 10 // Cho cuộn 10 năm về sau
        binding.pickerYear.value = initialYear // Đặt giá trị năm hiện tại

        // Xây dựng Dialog
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding.root) // Gắn layout vào dialog

        // Xử lý nút "Xác nhận"
        binding.buttonConfirm.setOnClickListener {
            val selectedYear = binding.pickerYear.value
            val selectedMonth = binding.pickerMonth.value

            // Gửi dữ liệu về Fragment
            listener.onMonthYearSelected(selectedYear, selectedMonth)
            dismiss() // Đóng dialog
        }

        return builder.create()
    }
}