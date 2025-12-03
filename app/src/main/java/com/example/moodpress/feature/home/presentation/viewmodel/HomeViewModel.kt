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
import java.util.Date

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

    // 1. State để lưu trữ tháng/năm đang chọn
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    // 2. State để gửi cho UI
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDataForCurrentMonth()
    }

    fun loadDataForCurrentMonth() {
        val year = _selectedDate.value.get(Calendar.YEAR)
        val month = _selectedDate.value.get(Calendar.MONTH) + 1
        loadData(year, month)
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
                // 1. Lấy dữ liệu thô từ UseCase (gọi API)
                val entries = getJournalHistoryUseCase(year, month)

                // 2. Xử lý dữ liệu cho Calendar
                val calendarDays = generateCalendarDays(year, month, entries)

                // 3. Gửi 2 danh sách về cho UI
                _uiState.value = HomeUiState.Success(
                    journalList = entries.sortedByDescending { it.timestamp }, // Sắp xếp cho List
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
                // Tải lại dữ liệu cho tháng hiện tại
                loadDataForCurrentMonth()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Lỗi khi xóa")
            }
        }
    }

    private fun generateCalendarDays(
        year: Int,
        month: Int, // (1-12)
        entries: List<JournalEntry>
    ): List<CalendarDay> {

        // A. Map dữ liệu (Ngày -> Cảm xúc cuối cùng của ngày đó)
        val emotionMap = mutableMapOf<Int, String>()
        entries.sortedBy { it.timestamp }.forEach { entry ->
            val cal = Calendar.getInstance()
            cal.time = entry.timestamp
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            emotionMap[dayOfMonth] = entry.emotion
        }

        // B. Tạo lưới lịch
        val calendarDays = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance(Locale("vi", "VN"))
        cal.set(year, month - 1, 1, 0, 0, 0)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Căn chỉnh Thứ Hai là ngày đầu tuần
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        firstDayOfWeek = if (firstDayOfWeek == 1) 7 else firstDayOfWeek - 1

        // Lấy số ngày của tháng trước
        val prevMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 1. Thêm các ngày mờ của tháng trước
        for (i in (firstDayOfWeek - 2) downTo 0) {
            val day = daysInPrevMonth - i
            val date = (prevMonthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.time
            calendarDays.add(CalendarDay(date = date, dayOfMonth = day.toString(), isCurrentMonth = false, emotion = null))
        }

        // 2. Thêm các ngày của tháng này
        val currentMonthCal = cal.clone() as Calendar
        for (i in 1..daysInMonth) {
            val date = (currentMonthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, i) }.time
            calendarDays.add(
                CalendarDay(
                    date = date,
                    dayOfMonth = i.toString(),
                    isCurrentMonth = true,
                    emotion = emotionMap[i] // Lấy cảm xúc
                )
            )
        }

        // 3. Thêm các ngày mờ của tháng sau
        val nextMonthCal = cal.clone() as Calendar
        nextMonthCal.add(Calendar.MONTH, 1)
        val daysToAdd = 42 - calendarDays.size // (Lưới 6x7 = 42)
        for (i in 1..daysToAdd) {
            calendarDays.add(CalendarDay(date = nextMonthCal.time, i.toString(), isCurrentMonth = false, emotion = null))
        }

        return calendarDays
    }
}