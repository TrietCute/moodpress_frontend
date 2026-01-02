package com.example.moodpress.feature.stats.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.stats.data.repository.StatsRepository
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class StatsUiState {
    data object Loading : StatsUiState()
    data class Success(val stats: WeeklyStats) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

data class NavigationState(
    val isPrevEnabled: Boolean = false,
    val isNextEnabled: Boolean = false,
    val currentLabel: String = "Đang tải..."
)

@HiltViewModel
class WeeklyStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _navState = MutableStateFlow(NavigationState())
    val navState: StateFlow<NavigationState> = _navState.asStateFlow()

    // Date Management
    private val todayStartOfWeek: Calendar = getStartOfWeek(Calendar.getInstance())
    private val currentViewStartDate: Calendar = todayStartOfWeek.clone() as Calendar

    var firstEntryDate: Date? = null
        private set

    init {
        fetchFirstEntryDate()
        loadWeeklyStats(todayStartOfWeek.time)
    }

    private fun fetchFirstEntryDate() {
        viewModelScope.launch {
            try {
                firstEntryDate = statsRepository.getFirstJournalDate()
                updateNavigationState()
            } catch (_: Exception) { }
        }
    }

    fun onPrevWeekClicked() {
        currentViewStartDate.add(Calendar.WEEK_OF_YEAR, -1)
        loadWeeklyStats(currentViewStartDate.time)
    }

    fun onNextWeekClicked() {
        currentViewStartDate.add(Calendar.WEEK_OF_YEAR, 1)
        loadWeeklyStats(currentViewStartDate.time)
    }

    fun loadWeeklyStats(startDate: Date) {
        _uiState.value = StatsUiState.Loading

        // Sync internal calendar state with requested date
        currentViewStartDate.time = startDate
        updateNavigationState()

        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateString = sdf.format(startDate)

                val stats = statsRepository.getWeeklyStats(dateString)
                _uiState.value = StatsUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = StatsUiState.Error(e.message ?: "Lỗi")
            }
        }
    }

    private fun updateNavigationState() {
        val sdf = SimpleDateFormat("dd/MM", Locale("vi", "VN"))

        // 1. Calculate Label
        val endOfWeek = (currentViewStartDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
        }

        val label = if (isSameDay(currentViewStartDate, todayStartOfWeek)) {
            "7 ngày gần đây"
        } else {
            "${sdf.format(currentViewStartDate.time)} - ${sdf.format(endOfWeek.time)}"
        }

        // 2. Calculate Buttons State
        val canGoPrev = firstEntryDate?.let { date ->
            val firstWeekStart = getStartOfWeek(Calendar.getInstance().apply { time = date })
            currentViewStartDate.after(firstWeekStart)
        } ?: false

        val canGoNext = currentViewStartDate.before(todayStartOfWeek)

        _navState.value = NavigationState(
            isPrevEnabled = canGoPrev,
            isNextEnabled = canGoNext,
            currentLabel = label
        )
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getStartOfWeek(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}