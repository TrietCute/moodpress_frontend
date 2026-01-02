package com.example.moodpress.feature.stats.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.databinding.DialogWeekSelectionBinding
import com.example.moodpress.feature.stats.domain.model.WeekOption
import com.example.moodpress.feature.stats.presentation.view.adapter.WeekAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekSelectionBottomSheet(
    private val firstEntryDate: Date?,
    private val onWeekSelected: (WeekOption) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogWeekSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWeekSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val weekOptions = generateWeekOptions()

        binding.recyclerWeeks.layoutManager = LinearLayoutManager(context)
        binding.recyclerWeeks.adapter = WeekAdapter(weekOptions) { selected ->
            onWeekSelected(selected)
            dismiss()
        }
    }

    private fun generateWeekOptions(): List<WeekOption> {
        val options = mutableListOf<WeekOption>()
        val localeVi = Locale("vi", "VN")

        // Base calendar for calculations (Today at 00:00:00)
        val today = Calendar.getInstance(localeVi).apply { stripTime() }

        // --- OPTION 1: 7 Days ---
        val last7DaysStart = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        options.add(WeekOption("7 ngày gần đây", last7DaysStart.time, true))

        if (firstEntryDate == null) return options

        // --- OPTION 2+: Past Weeks ---
        val currentWeekMonday = getMondayOfWeek(today)

        val firstEntryCal = Calendar.getInstance(localeVi).apply { time = firstEntryDate }
        val startMonday = getMondayOfWeek(firstEntryCal)

        val historyOptions = mutableListOf<WeekOption>()
        val iteratorCal = startMonday.clone() as Calendar
        val sdf = SimpleDateFormat("dd/MM", localeVi)

        while (!iteratorCal.after(currentWeekMonday)) {
            val start = iteratorCal.time
            val endCal = (iteratorCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }

            val label = "Tuần: ${sdf.format(start)} - ${sdf.format(endCal.time)}"
            historyOptions.add(WeekOption(label, start, false))

            iteratorCal.add(Calendar.DAY_OF_YEAR, 7)
        }

        options.addAll(historyOptions.reversed())
        return options
    }

    private fun getMondayOfWeek(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            // Force start of week to Monday regardless of device Locale
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            stripTime()
        }
    }

    private fun Calendar.stripTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}