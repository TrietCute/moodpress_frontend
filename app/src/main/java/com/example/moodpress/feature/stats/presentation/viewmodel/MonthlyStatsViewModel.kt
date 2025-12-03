package com.example.moodpress.feature.stats.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.stats.domain.model.DailyMood
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.data.repository.StatsRepository
import com.example.moodpress.feature.stats.presentation.view.adapter.MonthDayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val uiState: StateFlow<MonthlyUiState> = _uiState

    // Ngày đầu tiên viết nhật ký (để giới hạn lịch)
    var firstEntryDate: Date? = null
        private set

    // Trạng thái hiện tại
    private var currentStartDate: Calendar = getStartOfMonth(Calendar.getInstance())
    private var is30DaysMode = false
    private var currentFilterEmotion: String? = null

    // Cache dữ liệu gốc để lọc local
    private var rawStats: WeeklyStats? = null

    init {
        fetchFirstEntryDate()
        loadMonthlyStats(isLast30Days = false)
    }

    private fun fetchFirstEntryDate() {
        viewModelScope.launch {
            try {
                firstEntryDate = statsRepository.getFirstJournalDate()
            } catch (e: Exception) { }
        }
    }

    fun loadMonthlyStats(isLast30Days: Boolean, specificDate: Date? = null) {
        _uiState.value = MonthlyUiState.Loading
        is30DaysMode = isLast30Days

        val (start, end) = if (isLast30Days) {
            val endCal = Calendar.getInstance()
            val startCal = (endCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -29) }
            startCal to endCal
        } else {
            if (specificDate != null) {
                currentStartDate.time = specificDate
            }
            currentStartDate.set(Calendar.DAY_OF_MONTH, 1)

            val startCal = currentStartDate.clone() as Calendar
            val endCal = (currentStartDate.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            startCal to endCal
        }

        currentStartDate = start.clone() as Calendar

        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val stats = statsRepository.getMonthlyStats(
                    startDate = sdf.format(start.time),
                    endDate = sdf.format(end.time)
                )

                rawStats = stats
                processDataForUi(stats, start, end)

            } catch (e: Exception) {
                _uiState.value = MonthlyUiState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    private fun processDataForUi(stats: WeeklyStats, startCal: Calendar, endCal: Calendar) {
        val daysList = mutableListOf<MonthDayUiModel>()
        val moodMap = mutableMapOf<Int, String>()
        stats.dailyMoods.forEach { dailyMood ->
            if (currentFilterEmotion == null || dailyMood.emotion == currentFilterEmotion) {
                val cal = Calendar.getInstance().apply { time = dailyMood.date }
                moodMap[cal.get(Calendar.DAY_OF_MONTH)] = dailyMood.emotion
            }
        }

        if (!is30DaysMode) {
            val firstDayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
            val emptyCells = if (firstDayOfWeek == 1) 6 else firstDayOfWeek - 2

            for (i in 0 until emptyCells) {
                daysList.add(MonthDayUiModel("", null))
            }
        }

        val tempCal = startCal.clone() as Calendar
        while (!tempCal.after(endCal)) {
            val dayOfMonth = tempCal.get(Calendar.DAY_OF_MONTH)

            val emotion = findEmotionForDate(stats.dailyMoods, tempCal.time)

            var isDimmed = false
            var finalEmotion: String? = null

            if (currentFilterEmotion != null) {
                // Đang lọc
                if (emotion == currentFilterEmotion) {
                    finalEmotion = emotion
                } else {
                    isDimmed = true // Làm mờ ngày không khớp
                }
            } else {
                // Không lọc
                finalEmotion = emotion
            }

            daysList.add(MonthDayUiModel(
                dayValue = dayOfMonth.toString(),
                emotion = finalEmotion,
                isDimmed = isDimmed
            ))

            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Tạo Label
        val label = if (is30DaysMode) "30 ngày gần đây" else {
            val sdf = SimpleDateFormat("MMMM, yyyy", Locale("vi", "VN"))
            sdf.format(startCal.time)
        }

        _uiState.value = MonthlyUiState.Success(stats, daysList, label)
    }

    private fun findEmotionForDate(dailyMoods: List<DailyMood>, date: Date): String? {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val targetKey = sdf.format(date)
        return dailyMoods.find { sdf.format(it.date) == targetKey }?.emotion
    }

    fun filterByEmotion(emotion: String?) {
        currentFilterEmotion = emotion
        rawStats?.let {
            loadMonthlyStats(is30DaysMode, currentStartDate.time)
        }
    }

    fun onPrevMonthClicked() {
        if (is30DaysMode) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            loadMonthlyStats(false, cal.time)
            return
        }
        if (firstEntryDate != null) {
            val firstCal = Calendar.getInstance().apply { time = firstEntryDate!! }

            // Nếu tháng hiện tại ĐÃ LÀ tháng đầu tiên (hoặc nhỏ hơn) -> Không lùi nữa
            if (isSameMonth(currentStartDate, firstCal) || currentStartDate.before(firstCal)) {
                return // Chặn
            }
        }
            currentStartDate.add(Calendar.MONTH, -1)
            loadMonthlyStats(false, currentStartDate.time)
    }

    fun onNextMonthClicked() {
        if (is30DaysMode) return
        val today = Calendar.getInstance()
        if (isSameMonth(currentStartDate, today) || currentStartDate.after(today)) {
            return // Chặn
        }

        currentStartDate.add(Calendar.MONTH, 1)
        loadMonthlyStats(false, currentStartDate.time)
    }

    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun getStartOfMonth(cal: Calendar): Calendar {
        val newCal = cal.clone() as Calendar
        newCal.set(Calendar.DAY_OF_MONTH, 1)
        newCal.set(Calendar.HOUR_OF_DAY, 0)
        newCal.set(Calendar.MINUTE, 0)
        newCal.set(Calendar.SECOND, 0)
        return newCal
    }

    fun getCurrentViewDate(): Calendar = currentStartDate
}