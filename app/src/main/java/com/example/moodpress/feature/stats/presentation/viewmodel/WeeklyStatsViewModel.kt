package com.example.moodpress.feature.stats.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.data.repository.StatsRepository
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
    val uiState: StateFlow<StatsUiState> = _uiState

    private val _navState = MutableStateFlow(NavigationState())
    val navState: StateFlow<NavigationState> = _navState.asStateFlow()

    private var currentViewStartDate: Calendar = getStartOfWeek(Calendar.getInstance())
    private val todayStartOfWeek: Calendar = getStartOfWeek(Calendar.getInstance())

    var firstEntryDate: Date? = null
        private set

    init {
        fetchFirstEntryDate()
        loadWeeklyStats(getStartOfWeek(Calendar.getInstance()).time)
    }

    private fun fetchFirstEntryDate() {
        viewModelScope.launch {
            try {
                val responseDto = statsRepository.getFirstJournalDate()
                firstEntryDate = responseDto
                updateNavigationState()

                android.util.Log.d("StatsDebug", "First Date: $firstEntryDate")
            } catch (e: Exception) {
                android.util.Log.e("StatsDebug", "Lỗi lấy ngày đầu: ${e.message}")
            }
        }
    }

    // Biến lưu ngày bắt đầu của tuần đang xem
    private var currentStartDate: Calendar = getStartOfWeek(Calendar.getInstance())

    init {
        fetchFirstEntryDate()
        loadWeeklyStats(todayStartOfWeek.time)
    }

    fun onPrevWeekClicked() {
        // Lùi lại 7 ngày
        currentViewStartDate.add(Calendar.DAY_OF_YEAR, -7)
        loadWeeklyStats(currentViewStartDate.time)
    }

    fun onNextWeekClicked() {
        // Tiến lên 7 ngày
        currentViewStartDate.add(Calendar.DAY_OF_YEAR, 7)
        loadWeeklyStats(currentViewStartDate.time)
    }

    private fun updateNavigationState() {
        val sdf = SimpleDateFormat("dd/MM", Locale("vi", "VN"))

        // 1. Tạo Label (Ví dụ: 01/11 - 07/11)
        val endOfWeek = (currentViewStartDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        val label = if (isSameDay(currentViewStartDate, todayStartOfWeek)) {
            "7 ngày gần đây" // Hoặc "Tuần này"
        } else {
            "${sdf.format(currentViewStartDate.time)} - ${sdf.format(endOfWeek.time)}"
        }

        // 2. Tính toán nút Prev
        // Cho phép lùi nếu: Chưa có ngày đầu (null) HOẶC Tuần đang xem > Tuần đầu tiên
        val canGoPrev = if (firstEntryDate == null) {
            false
        } else {
            val firstWeekStart = getStartOfWeek(Calendar.getInstance().apply { time = firstEntryDate })
            currentViewStartDate.after(firstWeekStart)
        }

        // 3. Tính toán nút Next
        // Cho phép tiến nếu: Tuần đang xem < Tuần hiện tại
        val canGoNext = currentViewStartDate.before(todayStartOfWeek)

        _navState.value = NavigationState(
            isPrevEnabled = canGoPrev,
            isNextEnabled = canGoNext,
            currentLabel = label
        )
    }

    // Helper so sánh ngày
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getStartOfWeek(cal: Calendar): Calendar {
        val newCal = cal.clone() as Calendar
        // Đặt ngày đầu tuần là Thứ 2
        newCal.firstDayOfWeek = Calendar.MONDAY
        newCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        // Reset giờ
        newCal.set(Calendar.HOUR_OF_DAY, 0)
        newCal.set(Calendar.MINUTE, 0)
        newCal.set(Calendar.SECOND, 0)
        return newCal
    }

    fun loadWeeklyStats(startDate: Date) {
        _uiState.value = StatsUiState.Loading
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
}