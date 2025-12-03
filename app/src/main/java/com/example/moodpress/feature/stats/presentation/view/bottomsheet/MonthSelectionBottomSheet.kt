package com.example.moodpress.feature.stats.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moodpress.databinding.DialogMonthSelectionBinding // (Tạo layout xml này)
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar
import java.util.Date

class MonthSelectionBottomSheet(
    private val firstEntryDate: Date?,
    private val onOptionSelected: (isLast30Days: Boolean, date: Date?) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogMonthSelectionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogMonthSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút 30 ngày
        binding.btnLast30Days.setOnClickListener {
            onOptionSelected(true, null)
            dismiss()
        }

        // Nút Tháng này
        binding.btnThisMonth.setOnClickListener {
            onOptionSelected(false, Date()) // Date() là hôm nay -> Tháng này
            dismiss()
        }

        // Cấu hình Picker (Tháng/Năm)
        setupPickers()

        // Nút Xác nhận Picker
        binding.btnConfirmPicker.setOnClickListener {
            val year = binding.pickerYear.value
            val month = binding.pickerMonth.value // 0-11

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)

            onOptionSelected(false, cal.time)
            dismiss()
        }
    }

    private fun setupPickers() {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)

        val startCal = Calendar.getInstance()
        if (firstEntryDate != null) {
            startCal.time = firstEntryDate!!
        } else {
            startCal.set(Calendar.YEAR, currentYear) // Mặc định năm nay nếu chưa có dữ liệu
        }
        val startYear = startCal.get(Calendar.YEAR)

        // Setup Năm
        binding.pickerYear.minValue = startYear
        binding.pickerYear.maxValue = currentYear
        binding.pickerYear.value = currentYear
        binding.pickerYear.wrapSelectorWheel = false

        // Setup Tháng
        val months = arrayOf("Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12")
        binding.pickerMonth.displayedValues = months
        binding.pickerMonth.minValue = 0
        binding.pickerMonth.maxValue = 11
        binding.pickerMonth.value = today.get(Calendar.MONTH)

        // Logic chặn tháng: Nếu chọn Năm hiện tại -> Không cho chọn tháng tương lai
        binding.pickerYear.setOnValueChangedListener { _, _, newYear ->
            if (newYear == currentYear) {
                binding.pickerMonth.maxValue = today.get(Calendar.MONTH)
            } else {
                binding.pickerMonth.maxValue = 11
            }
        }
    }
}