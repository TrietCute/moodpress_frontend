package com.example.moodpress.feature.home.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentHomeBinding
import com.example.moodpress.feature.home.domain.model.CalendarDay // (Kiểm tra import)
import com.example.moodpress.feature.home.presentation.viewmodel.HomeUiState
import com.example.moodpress.feature.home.presentation.viewmodel.HomeViewModel
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.feature.journal.presentation.view.JournalOptionsBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

@AndroidEntryPoint
class HomeFragment : Fragment(),
    MonthYearPickerDialog.OnMonthYearSelectedListener,
    JournalListAdapter.OnJournalActionsListener,
    JournalOptionsBottomSheet.OptionsListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private var isListView = true

    private lateinit var journalListAdapter: JournalListAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    private var entryToActOn: JournalEntry? = null
    private var fullJournalList: List<JournalEntry> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        updateViewVisibility()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDataForCurrentMonth()
    }

    private fun setupRecyclerViews() {
        journalListAdapter = JournalListAdapter(this)
        binding.journalListRecyclerView.adapter = journalListAdapter
        binding.journalListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        calendarAdapter = CalendarAdapter { day ->
            onDayClicked(day)
        }
        binding.journalCalendarRecyclerView.adapter = calendarAdapter
        binding.journalCalendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
    }

    private fun setupClickListeners() {
        binding.datePickerButton.setOnClickListener {
            showMonthYearPicker()
        }
        binding.viewToggleButton.setOnClickListener {
            isListView = !isListView
            updateViewVisibility()
            if (isListView) {
                resetListFilter()
            }
        }
        binding.fabAddJournal.setOnClickListener {
            safeNavigate(HomeFragmentDirections.actionHomeFragmentToCreateJournalFragment(null, -1L))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HomeUiState.Success -> {
                        fullJournalList = state.journalList

                        // Cập nhật List Adapter
                        journalListAdapter.submitList(state.journalList)

                        calendarAdapter.updateDays(state.calendarDays)
                    }
                    is HomeUiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    is HomeUiState.Loading -> {
                        // (Hiển thị ProgressBar...)
                    }
                }
            }
        }

        // Lắng nghe ngày tháng được chọn (để cập nhật nút)
        lifecycleScope.launch {
            viewModel.selectedDate.collect { calendar ->
                updateDateButtonText(calendar)
            }
        }
    }

    private fun safeNavigate(directions: androidx.navigation.NavDirections) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.nav_home) {
            navController.navigate(directions)
        }
    }

    private fun updateViewVisibility() {
        if (isListView) {
            binding.journalListRecyclerView.isVisible = true
            binding.calendarContainer.isVisible = false
            binding.viewToggleButton.setImageResource(R.drawable.ic_view_list)
        } else {
            binding.journalListRecyclerView.isVisible = false
            binding.calendarContainer.isVisible = true
            binding.viewToggleButton.setImageResource(R.drawable.ic_calendar)
        }
    }

    private fun resetListFilter() {
        if (journalListAdapter.currentList != fullJournalList) {
            journalListAdapter.submitList(fullJournalList)
        }
    }

    private fun updateDateButtonText(calendar: Calendar) {
        val sdf = SimpleDateFormat("MMMM, yyyy", Locale("vi", "VN"))
        val formattedDate = sdf.format(calendar.time)
        binding.datePickerButton.text = formattedDate
    }

    private fun showMonthYearPicker() {
        val currentCal = viewModel.selectedDate.value
        val dialog = MonthYearPickerDialog(
            initialYear = currentCal.get(Calendar.YEAR),
            initialMonth = currentCal.get(Calendar.MONTH), // 0-11
            listener = this
        )
        dialog.show(childFragmentManager, "MONTH_YEAR_PICKER")
    }

    private fun onDayClicked(day: CalendarDay) {
        if (!day.isCurrentMonth) {
            return
        }

        if (!isPastOrToday(day.date)) {
            Toast.makeText(requireContext(), "Không thể tạo nhật ký cho tương lai", Toast.LENGTH_SHORT).show()
            return
        }

        if (day.emotion == null) {
            val action = HomeFragmentDirections.actionHomeFragmentToCreateJournalFragment(
                journalId = null,
                selectedDate = day.date.time
            )
            safeNavigate(action)
        } else {
            val filteredList = fullJournalList.filter {
                isSameDay(it.timestamp, day.date)
            }
            journalListAdapter.submitList(filteredList)

            isListView = true
            updateViewVisibility()
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isPastOrToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { time = date }

        // Reset giờ của "hôm nay" về 00:00:00
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        selected.set(Calendar.HOUR_OF_DAY, 0)
        selected.set(Calendar.MINUTE, 0)
        selected.set(Calendar.SECOND, 0)
        selected.set(Calendar.MILLISECOND, 0)

        return !selected.after(today)
    }

    // --- CÁC HÀM CALLBACK (Lắng nghe sự kiện) ---

    override fun onMonthYearSelected(year: Int, month: Int) { // month (0-11)
        viewModel.updateSelectedDate(year, month)
        resetListFilter()
    }

    override fun onMoreOptionsClicked(entry: JournalEntry) {
        this.entryToActOn = entry
        val bottomSheet = JournalOptionsBottomSheet()
        bottomSheet.show(childFragmentManager, JournalOptionsBottomSheet.TAG)
    }

    override fun onEditClicked() {
        entryToActOn?.let { entry ->
            val action = HomeFragmentDirections.actionHomeFragmentToCreateJournalFragment(entry.id)
            safeNavigate(action)
        }
        entryToActOn = null
    }

    override fun onDeleteClicked() {
        entryToActOn?.let { entry ->
            showDeleteConfirmationDialog(entry)
        }
        entryToActOn = null
    }

    private fun showDeleteConfirmationDialog(entry: JournalEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa vĩnh viễn nhật ký này không?")
            .setNegativeButton("Hủy bỏ", null)
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteJournal(entry.id)
                Toast.makeText(requireContext(), "Đã xóa", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}