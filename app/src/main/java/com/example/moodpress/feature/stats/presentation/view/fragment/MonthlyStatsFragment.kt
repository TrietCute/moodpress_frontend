package com.example.moodpress.feature.stats.presentation.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.databinding.FragmentMonthlyStatsBinding
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.presentation.view.adapter.EmotionFilterAdapter
import com.example.moodpress.feature.stats.presentation.view.adapter.MonthlyCalendarAdapter
import com.example.moodpress.feature.stats.presentation.viewmodel.MonthlyStatsViewModel
import com.example.moodpress.feature.stats.presentation.viewmodel.MonthlyUiState
import com.example.moodpress.feature.stats.presentation.view.bottomsheet.MonthSelectionBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MonthlyStatsFragment : Fragment() {

    private var _binding: FragmentMonthlyStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MonthlyStatsViewModel by viewModels()

    private lateinit var calendarAdapter: MonthlyCalendarAdapter
    private lateinit var emotionFilterAdapter: EmotionFilterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMonthlyStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        calendarAdapter = MonthlyCalendarAdapter()
        binding.calendarGridRecycler.adapter = calendarAdapter
        binding.calendarGridRecycler.layoutManager = GridLayoutManager(context, 7)

        emotionFilterAdapter = EmotionFilterAdapter { emotionKey ->
            viewModel.filterByEmotion(emotionKey)
        }
        binding.recyclerEmotionFilter.apply {
            adapter = emotionFilterAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            rv.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    return false
                }

                override fun onTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: android.view.MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }    }

    private fun setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener { viewModel.onPrevMonthClicked() }
        binding.btnNextMonth.setOnClickListener { viewModel.onNextMonthClicked() }

        binding.btnSelectMonth.setOnClickListener {
            showMonthSelectionDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MonthlyUiState.Success -> {
                        updateStatBoxes(state.stats)

                        calendarAdapter.submitList(state.calendarDays)

                        binding.btnSelectMonth.text = state.label
                        binding.tvMonthFooter.text = "${state.label}\n${state.stats.totalEntries} mục nhập"

                        updateNavigationButtons()
                    }
                    is MonthlyUiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is MonthlyUiState.Loading -> {
                        // Show loading...
                    }
                }
            }
        }
    }

    private fun updateStatBoxes(stats: WeeklyStats) {
        // Box 1: Hiện tại
        binding.boxCurrentStreak.tvLabel.text = "Hiện tại"
        binding.boxCurrentStreak.tvValue.text = stats.currentStreak.toString()
        // binding.boxCurrentStreak.imgIcon.setImageResource(...)

        // Box 2: Dài nhất
        binding.boxLongestStreak.tvLabel.text = "Dài nhất"
        binding.boxLongestStreak.tvValue.text = stats.longestStreak.toString()

        // Box 3: Tổng số
        binding.boxTotalEntries.tvLabel.text = "Tổng số"
        binding.boxTotalEntries.tvValue.text = stats.allTimeTotal.toString()
    }

    private fun showMonthSelectionDialog() {
        val dialog = MonthSelectionBottomSheet(
            firstEntryDate = viewModel.firstEntryDate
        ) { isLast30Days, date ->
            viewModel.loadMonthlyStats(isLast30Days, date)
        }
        dialog.show(childFragmentManager, "MonthSelection")
    }

    private fun updateNavigationButtons() {
        val today = Calendar.getInstance()

        // Lấy ngày đang xem từ ViewModel (bạn cần expose currentStartDate ra public hoặc dùng getter)
        // Để đơn giản, ta lấy từ text của nút (hoặc bạn thêm field vào UiState)
        // Giả sử ViewModel có biến public 'currentViewDate': Calendar
        val currentDate = viewModel.getCurrentViewDate() // Bạn cần thêm hàm này vào VM
        val firstDate = viewModel.firstEntryDate

        // --- Logic Nút Next ---
        val canGoNext = !isSameMonth(currentDate, today) && currentDate.before(today)
        binding.btnNextMonth.isEnabled = canGoNext
        binding.btnNextMonth.alpha = if (canGoNext) 1.0f else 0.3f

        // --- Logic Nút Prev ---
        val canGoPrev = if (firstDate != null) {
            val firstCal = Calendar.getInstance().apply { time = firstDate }
            !isSameMonth(currentDate, firstCal) && currentDate.after(firstCal)
        } else {
            false
        }
        binding.btnPrevMonth.isEnabled = canGoPrev
        binding.btnPrevMonth.alpha = if (canGoPrev) 1.0f else 0.3f
    }

    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}