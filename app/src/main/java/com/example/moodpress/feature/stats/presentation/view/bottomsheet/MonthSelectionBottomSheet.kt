package com.example.moodpress.feature.stats.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moodpress.databinding.DialogMonthSelectionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar
import java.util.Date

class MonthSelectionBottomSheet(
    private val firstEntryDate: Date?,
    private val onOptionSelected: (isLast30Days: Boolean, date: Date?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogMonthSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogMonthSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPickers()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        with(binding) {
            btnLast30Days.setOnClickListener {
                onOptionSelected(true, null)
                dismiss()
            }

            btnThisMonth.setOnClickListener {
                onOptionSelected(false, Date())
                dismiss()
            }

            btnConfirmPicker.setOnClickListener {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, pickerYear.value)
                    set(Calendar.MONTH, pickerMonth.value)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                onOptionSelected(false, cal.time)
                dismiss()
            }
        }
    }

    private fun setupPickers() {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val currentMonthIndex = today.get(Calendar.MONTH)

        val startYear = firstEntryDate?.let { date ->
            Calendar.getInstance().apply { time = date }.get(Calendar.YEAR)
        } ?: currentYear

        with(binding.pickerYear) {
            minValue = startYear
            maxValue = currentYear
            value = currentYear
            wrapSelectorWheel = false

            setOnValueChangedListener { _, _, newYear ->
                updateMonthLimit(newYear, currentYear, currentMonthIndex)
            }
        }

        with(binding.pickerMonth) {
            displayedValues = Array(12) { i -> "Tháng ${i + 1}" }
            minValue = 0
            // Initial limit check
            maxValue = if (startYear == currentYear) currentMonthIndex else 11
            value = currentMonthIndex
        }
    }

    private fun updateMonthLimit(selectedYear: Int, currentYear: Int, maxMonthIndex: Int) {
        binding.pickerMonth.maxValue = if (selectedYear == currentYear) {
            maxMonthIndex
        } else {
            11
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}