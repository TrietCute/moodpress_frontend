package com.example.moodpress.feature.home.presentation.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.moodpress.databinding.DialogMonthYearPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class MonthYearPickerDialog(
    private val initialYear: Int,
    private val initialMonth: Int,
    private val listener: OnMonthYearSelectedListener
) : DialogFragment() {

    interface OnMonthYearSelectedListener {
        fun onMonthYearSelected(year: Int, month: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogMonthYearPickerBinding.inflate(layoutInflater)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        with(binding) {
            // Setup Month Picker
            pickerMonth.minValue = 0
            pickerMonth.maxValue = 11
            pickerMonth.displayedValues = Array(12) { "Tháng ${it + 1}" }
            pickerMonth.value = initialMonth

            // Setup Year Picker
            pickerYear.minValue = currentYear - 10
            pickerYear.maxValue = currentYear + 10
            pickerYear.value = initialYear

            buttonConfirm.setOnClickListener {
                listener.onMonthYearSelected(pickerYear.value, pickerMonth.value)
                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
}