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
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val labelDateFormat = SimpleDateFormat("MMMM, yyyy", Locale("vi", "VN"))

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

        val (startCal, endCal) = calculateDateRange(isLast30Days, specificDate)
        currentStartDate = startCal.clone() as Calendar

        viewModelScope.launch {
            try {
                val stats = statsRepository.getMonthlyStats(
                    startDate = apiDateFormat.format(startCal.time),
                    endDate = apiDateFormat.format(endCal.time)
                )

                rawStats = stats
                processDataForUi(stats, startCal, endCal)

            } catch (e: Exception) {
                _uiState.value = MonthlyUiState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    private fun calculateDateRange(isLast30Days: Boolean, specificDate: Date?): Pair<Calendar, Calendar> {
        val endCal = Calendar.getInstance()

        return if (isLast30Days) {
            val startCal = (endCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -29) }
            startCal to endCal
        } else {
            val startCal = if (specificDate != null) {
                Calendar.getInstance().apply { time = specificDate }
            } else {
                currentStartDate.clone() as Calendar
            }
            startCal.set(Calendar.DAY_OF_MONTH, 1)

            endCal.time = startCal.time
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))

            startCal to endCal
        }
    }

    private fun processDataForUi(stats: WeeklyStats, startCal: Calendar, endCal: Calendar) {
        val daysList = mutableListOf<MonthDayUiModel>()
        val moodMap = stats.dailyMoods.associate {
            apiDateFormat.format(it.date) to it.emotion
        }

        if (!is30DaysMode) {
            val firstDayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfWeek + 5) % 7
            repeat(emptyCells) {
                daysList.add(MonthDayUiModel("", null))
            }
        }

        val iteratorCal = startCal.clone() as Calendar
        while (!iteratorCal.after(endCal)) {
            val dateKey = apiDateFormat.format(iteratorCal.time)
            val emotion = moodMap[dateKey]
            val dayOfMonth = iteratorCal.get(Calendar.DAY_OF_MONTH)
            val (finalEmotion, isDimmed) = applyFilter(emotion)

            daysList.add(
                MonthDayUiModel(
                    dayValue = dayOfMonth.toString(),
                    emotion = finalEmotion,
                    isDimmed = isDimmed
                )
            )
            iteratorCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val label = if (is30DaysMode) "30 ngày gần đây" else labelDateFormat.format(startCal.time)
        _uiState.value = MonthlyUiState.Success(stats, daysList, label)
    }

    private fun applyFilter(emotion: String?): Pair<String?, Boolean> {
        val filter = currentFilterEmotion ?: return emotion to false

        return if (emotion == filter) {
            emotion to false
        } else {
            null to true
        }
    }

    fun filterByEmotion(emotion: String?) {
        currentFilterEmotion = emotion
        rawStats?.let { stats ->
            val (_, endCal) = calculateDateRange(is30DaysMode, null)
            processDataForUi(stats, currentStartDate, endCal)
        }
    }

    fun onPrevMonthClicked() {
        if (is30DaysMode) {
            val prevMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            loadMonthlyStats(false, prevMonth.time)
            return
        }

        firstEntryDate?.let { firstDate ->
            val firstCal = Calendar.getInstance().apply { time = firstDate }
            if (isSameMonth(currentStartDate, firstCal) || currentStartDate.before(firstCal)) return
        }

        currentStartDate.add(Calendar.MONTH, -1)
        loadMonthlyStats(false, currentStartDate.time)
    }

    fun onNextMonthClicked() {
        if (is30DaysMode) return

        val today = Calendar.getInstance()
        if (isSameMonth(currentStartDate, today) || currentStartDate.after(today)) return

        currentStartDate.add(Calendar.MONTH, 1)
        loadMonthlyStats(false, currentStartDate.time)
    }

    // --- Helpers ---

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