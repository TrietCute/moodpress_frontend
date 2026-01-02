package com.example.moodpress.feature.stats.presentation.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.databinding.FragmentMonthlyStatsBinding
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.presentation.view.adapter.EmotionFilterAdapter
import com.example.moodpress.feature.stats.presentation.view.adapter.MonthlyCalendarAdapter
import com.example.moodpress.feature.stats.presentation.view.bottomsheet.MonthSelectionBottomSheet
import com.example.moodpress.feature.stats.presentation.viewmodel.MonthlyStatsViewModel
import com.example.moodpress.feature.stats.presentation.viewmodel.MonthlyUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MonthlyStatsFragment : Fragment() {

    private var _binding: FragmentMonthlyStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MonthlyStatsViewModel by viewModels()

    private val calendarAdapter by lazy { MonthlyCalendarAdapter() }
    private val emotionFilterAdapter by lazy {
        EmotionFilterAdapter { emotionKey -> viewModel.filterByEmotion(emotionKey) }
    }

    private val horizontalScrollTouchListener = object : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_DOWN) {
                rv.parent.requestDisallowInterceptTouchEvent(true)
            }
            return false
        }
        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

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
        with(binding.calendarGridRecycler) {
            adapter = calendarAdapter
            layoutManager = GridLayoutManager(context, 7)
        }

        with(binding.recyclerEmotionFilter) {
            adapter = emotionFilterAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addOnItemTouchListener(horizontalScrollTouchListener)
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            btnPrevMonth.setOnClickListener { viewModel.onPrevMonthClicked() }
            btnNextMonth.setOnClickListener { viewModel.onNextMonthClicked() }
            btnSelectMonth.setOnClickListener { showMonthSelectionDialog() }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: MonthlyUiState) {
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
            is MonthlyUiState.Loading -> { }
        }
    }

    private fun updateStatBoxes(stats: WeeklyStats) {
        with(binding) {
            boxCurrentStreak.tvLabel.text = "Hiện tại"
            boxCurrentStreak.tvValue.text = stats.currentStreak.toString()

            boxLongestStreak.tvLabel.text = "Dài nhất"
            boxLongestStreak.tvValue.text = stats.longestStreak.toString()

            boxTotalEntries.tvLabel.text = "Tổng số"
            boxTotalEntries.tvValue.text = stats.allTimeTotal.toString()
        }
    }

    private fun updateNavigationButtons() {
        val today = Calendar.getInstance()
        val currentDate = viewModel.getCurrentViewDate()
        val firstDate = viewModel.firstEntryDate

        val canGoNext = !isSameMonth(currentDate, today) && currentDate.before(today)
        updateButtonState(binding.btnNextMonth, canGoNext)

        val canGoPrev = firstDate?.let { date ->
            val firstCal = Calendar.getInstance().apply { time = date }
            !isSameMonth(currentDate, firstCal) && currentDate.after(firstCal)
        } ?: false
        updateButtonState(binding.btnPrevMonth, canGoPrev)
    }

    private fun updateButtonState(view: View, isEnabled: Boolean) {
        view.isEnabled = isEnabled
        view.alpha = if (isEnabled) 1.0f else 0.3f
    }

    private fun showMonthSelectionDialog() {
        MonthSelectionBottomSheet(
            firstEntryDate = viewModel.firstEntryDate
        ) { isLast30Days, date ->
            viewModel.loadMonthlyStats(isLast30Days, date)
        }.show(childFragmentManager, "MonthSelection")
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