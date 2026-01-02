package com.example.moodpress.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.home.domain.model.CalendarDay
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.feature.journal.domain.usecase.DeleteJournalUseCase
import com.example.moodpress.feature.journal.domain.usecase.GetJournalHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val journalList: List<JournalEntry>,
        val calendarDays: List<CalendarDay>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getJournalHistoryUseCase: GetJournalHistoryUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDataForCurrentMonth()
    }

    fun loadDataForCurrentMonth() {
        val cal = _selectedDate.value
        loadData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun updateSelectedDate(year: Int, month: Int) {
        _selectedDate.value = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        loadData(year, month + 1)
    }

    private fun loadData(year: Int, month: Int) {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            try {
                val entries = getJournalHistoryUseCase(year, month)
                val calendarDays = generateCalendarDays(year, month, entries)

                _uiState.value = HomeUiState.Success(
                    journalList = entries.sortedByDescending { it.timestamp },
                    calendarDays = calendarDays
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Không thể tải dữ liệu")
            }
        }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch {
            try {
                deleteJournalUseCase(id)
                loadDataForCurrentMonth()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Lỗi khi xóa")
            }
        }
    }

    private fun generateCalendarDays(
        year: Int,
        month: Int,
        entries: List<JournalEntry>
    ): List<CalendarDay> {
        // 1. Map Data: DayOfMonth -> Emotion (Last entry of day wins)
        val emotionMap = entries.sortedBy { it.timestamp }.associate { entry ->
            val cal = Calendar.getInstance().apply { time = entry.timestamp }
            cal.get(Calendar.DAY_OF_MONTH) to entry.emotion
        }

        // 2. Setup Calendar Pointer
        // Start from the 1st day of the requested month
        val iteratorCal = Calendar.getInstance(Locale("vi", "VN")).apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 3. Calculate Start Offset (To make Monday the first day)
        // Calendar.SUNDAY = 1, MONDAY = 2 ...
        // If Sun(1) -> back 6 days. If Mon(2) -> back 0 days.
        val dayOfWeek = iteratorCal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        iteratorCal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        // 4. Generate Grid (Standard 6 rows * 7 cols = 42 cells)
        val calendarDays = mutableListOf<CalendarDay>()
        repeat(42) {
            val date = iteratorCal.time
            val dayVal = iteratorCal.get(Calendar.DAY_OF_MONTH)

            // Check if this cell belongs to the currently selected month
            // Note: Month in Calendar is 0-indexed, so we compare with (month - 1)
            val isTargetMonth = iteratorCal.get(Calendar.MONTH) == (month - 1)

            calendarDays.add(
                CalendarDay(
                    date = date,
                    dayOfMonth = dayVal.toString(),
                    isCurrentMonth = isTargetMonth,
                    emotion = if (isTargetMonth) emotionMap[dayVal] else null
                )
            )

            // Move to next day
            iteratorCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendarDays
    }
}