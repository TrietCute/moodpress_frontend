package com.example.moodpress.feature.stats.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.stats.data.repository.StatsRepository
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.presentation.view.adapter.MonthDayUiModel
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

sealed class MonthlyUiState {
    data object Loading : MonthlyUiState()
    data class Success(
        val stats: WeeklyStats,
        val calendarDays: List<MonthDayUiModel>,
        val label: String
    ) : MonthlyUiState()
    data class Error(val message: String) : MonthlyUiState()
}

@HiltViewModel
class MonthlyStatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MonthlyUiState>(MonthlyUiState.Loading)
    val uiState: StateFlow<MonthlyUiState> = _uiState.asStateFlow()

    var firstEntryDate: Date? = null
        private set

    private var currentStartDate: Calendar = getStartOfMonth(Calendar.getInstance())
    private var is30DaysMode = false
    private var currentFilterEmotion: String? = null
    private var rawStats: WeeklyStats? = null

    init {
        fetchFirstEntryDate()
        loadMonthlyStats(isLast30Days = false)
    }

    private fun fetchFirstEntryDate() {
        viewModelScope.launch {
            try {
                firstEntryDate = statsRepository.getFirstJournalDate()
            } catch (_: Exception) { }
        }
    }

    fun loadMonthlyStats(isLast30Days: Boolean, specificDate: Date? = null) {
        _uiState.value = MonthlyUiState.Loading
        is30DaysMode = isLast30Days

        if (specificDate != null && !isLast30Days) {
            currentStartDate.time = specificDate
            currentStartDate.set(Calendar.DAY_OF_MONTH, 1)
        }

        val (startCal, endCal) = calculateDateRange(isLast30Days)

        // Sync internal state if not 30 days mode (already synced above if specificDate provided)
        if (!isLast30Days) {
            currentStartDate = startCal.clone() as Calendar
        }

        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val stats = statsRepository.getMonthlyStats(
                    startDate = sdf.format(startCal.time),
                    endDate = sdf.format(endCal.time)
                )

                rawStats = stats
                processDataForUi(stats, startCal, endCal)

            } catch (e: Exception) {
                _uiState.value = MonthlyUiState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    fun filterByEmotion(emotion: String?) {
        currentFilterEmotion = emotion
        rawStats?.let { stats ->
            // Re-process local data without API call
            val (start, end) = calculateDateRange(is30DaysMode)
            processDataForUi(stats, start, end)
        }
    }

    private fun processDataForUi(stats: WeeklyStats, startCal: Calendar, endCal: Calendar) {
        val daysList = mutableListOf<MonthDayUiModel>()

        // Optimization: Create a map for O(1) lookup instead of O(N) loop
        val moodMap = stats.dailyMoods.associate {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.DAY_OF_MONTH) to it.emotion
        }

        if (!is30DaysMode) {
            val firstDayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
            val emptyCells = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
            repeat(emptyCells) {
                daysList.add(MonthDayUiModel("", null))
            }
        }

        val tempCal = startCal.clone() as Calendar
        while (!tempCal.after(endCal)) {
            val dayOfMonth = tempCal.get(Calendar.DAY_OF_MONTH)
            val emotion = moodMap[dayOfMonth]

            val (finalEmotion, isDimmed) = applyFilterLogic(emotion)

            daysList.add(MonthDayUiModel(
                dayValue = dayOfMonth.toString(),
                emotion = finalEmotion,
                isDimmed = isDimmed
            ))

            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val label = if (is30DaysMode) "30 ngày gần đây" else {
            val sdf = SimpleDateFormat("MMMM, yyyy", Locale("vi", "VN"))
            sdf.format(startCal.time)
        }

        _uiState.value = MonthlyUiState.Success(stats, daysList, label)
    }

    private fun applyFilterLogic(emotion: String?): Pair<String?, Boolean> {
        if (currentFilterEmotion == null) return emotion to false

        return if (emotion == currentFilterEmotion) {
            emotion to false
        } else {
            emotion to true // Dimmed
        }
    }

    private fun calculateDateRange(isLast30Days: Boolean): Pair<Calendar, Calendar> {
        return if (isLast30Days) {
            val end = Calendar.getInstance()
            val start = (end.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -29) }
            start to end
        } else {
            val start = currentStartDate.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, 1)

            val end = currentStartDate.clone() as Calendar
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            start to end
        }
    }

    fun onPrevMonthClicked() {
        if (is30DaysMode) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            loadMonthlyStats(false, cal.time)
            return
        }

        if (canGoBack()) {
            currentStartDate.add(Calendar.MONTH, -1)
            loadMonthlyStats(false, currentStartDate.time)
        }
    }

    fun onNextMonthClicked() {
        if (is30DaysMode) return

        if (canGoForward()) {
            currentStartDate.add(Calendar.MONTH, 1)
            loadMonthlyStats(false, currentStartDate.time)
        }
    }

    private fun canGoBack(): Boolean {
        val firstDate = firstEntryDate ?: return true
        val firstCal = Calendar.getInstance().apply { time = firstDate }

        // Ensure strictly after first entry month
        return !isSameMonth(currentStartDate, firstCal) && currentStartDate.after(firstCal)
    }

    private fun canGoForward(): Boolean {
        val today = Calendar.getInstance()
        return !isSameMonth(currentStartDate, today) && currentStartDate.before(today)
    }

    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun getStartOfMonth(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun getCurrentViewDate(): Calendar = currentStartDate
}