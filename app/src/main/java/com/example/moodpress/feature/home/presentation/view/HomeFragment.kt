package com.example.moodpress.feature.home.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentHomeBinding
import com.example.moodpress.feature.home.domain.model.CalendarDay
import com.example.moodpress.feature.home.presentation.viewmodel.HomeUiState
import com.example.moodpress.feature.home.presentation.viewmodel.HomeViewModel
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.feature.journal.presentation.view.JournalOptionsBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment(),
    MonthYearPickerDialog.OnMonthYearSelectedListener,
    JournalListAdapter.OnJournalActionsListener,
    JournalOptionsBottomSheet.OptionsListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private var isListView = true
    private var entryToActOn: JournalEntry? = null
    private var fullJournalList: List<JournalEntry> = emptyList()

    private val journalListAdapter by lazy { JournalListAdapter(this) }
    private val calendarAdapter by lazy { CalendarAdapter { day -> onDayClicked(day) } }

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
        with(binding.journalListRecyclerView) {
            adapter = journalListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        with(binding.journalCalendarRecyclerView) {
            adapter = calendarAdapter
            layoutManager = GridLayoutManager(requireContext(), 7)
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            datePickerButton.setOnClickListener {
                showMonthYearPicker()
            }
            viewToggleButton.setOnClickListener {
                isListView = !isListView
                updateViewVisibility()
                if (isListView) resetListFilter()
            }
            fabAddJournal.setOnClickListener {
                safeNavigate(HomeFragmentDirections.actionHomeFragmentToCreateJournalFragment(null, -1L))
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeUiState() }
                launch { observeSelectedDate() }
            }
        }
    }

    private suspend fun observeUiState() {
        viewModel.uiState.collect { state ->
            when (state) {
                is HomeUiState.Success -> {
                    fullJournalList = state.journalList
                    journalListAdapter.submitList(state.journalList)
                    calendarAdapter.updateDays(state.calendarDays)
                }
                is HomeUiState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                is HomeUiState.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    private suspend fun observeSelectedDate() {
        viewModel.selectedDate.collect { calendar ->
            val sdf = SimpleDateFormat("MMMM, yyyy", Locale("vi", "VN"))
            binding.datePickerButton.text = sdf.format(calendar.time)
        }
    }

    private fun updateViewVisibility() {
        with(binding) {
            journalListRecyclerView.isVisible = isListView
            calendarContainer.isVisible = !isListView
            viewToggleButton.setIconResource(
                if (isListView) R.drawable.ic_view_list else R.drawable.ic_calendar
            )
        }
    }

    private fun onDayClicked(day: CalendarDay) {
        if (!day.isCurrentMonth) return

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
            val filteredList = fullJournalList.filter { isSameDay(it.timestamp, day.date) }
            journalListAdapter.submitList(filteredList)
            isListView = true
            updateViewVisibility()
        }
    }

    private fun isPastOrToday(date: Date): Boolean {
        val today = Calendar.getInstance().apply { stripTime() }
        val selected = Calendar.getInstance().apply {
            time = date
            stripTime()
        }
        return !selected.after(today)
    }

    private fun Calendar.stripTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showMonthYearPicker() {
        val currentCal = viewModel.selectedDate.value
        MonthYearPickerDialog(
            initialYear = currentCal.get(Calendar.YEAR),
            initialMonth = currentCal.get(Calendar.MONTH),
            listener = this
        ).show(childFragmentManager, "MONTH_YEAR_PICKER")
    }

    private fun safeNavigate(directions: androidx.navigation.NavDirections) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.nav_home) {
            navController.navigate(directions)
        }
    }

    private fun resetListFilter() {
        if (journalListAdapter.currentList != fullJournalList) {
            journalListAdapter.submitList(fullJournalList)
        }
    }

    // --- INTERFACE IMPLEMENTATIONS ---

    override fun onMonthYearSelected(year: Int, month: Int) {
        viewModel.updateSelectedDate(year, month)
        resetListFilter()
    }

    override fun onMoreOptionsClicked(entry: JournalEntry) {
        this.entryToActOn = entry
        JournalOptionsBottomSheet().show(childFragmentManager, JournalOptionsBottomSheet.TAG)
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