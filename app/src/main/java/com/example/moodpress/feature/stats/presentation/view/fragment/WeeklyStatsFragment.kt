package com.example.moodpress.feature.stats.presentation.view.fragment

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentWeeklyStatsBinding
import com.example.moodpress.feature.stats.domain.model.DailyMood
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.presentation.view.adapter.MoodCountAdapter
import com.example.moodpress.feature.stats.presentation.view.bottomsheet.WeekSelectionBottomSheet
import com.example.moodpress.feature.stats.presentation.viewmodel.NavigationState
import com.example.moodpress.feature.stats.presentation.viewmodel.StatsUiState
import com.example.moodpress.feature.stats.presentation.viewmodel.WeeklyStatsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.renderer.YAxisRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class WeeklyStatsFragment : Fragment() {

    private var _binding: FragmentWeeklyStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeeklyStatsViewModel by viewModels()
    private val moodAdapter by lazy { MoodCountAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeeklyStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupCharts()
        observeViewModel()
    }

    private fun setupViews() {
        // Recycler View
        with(binding.recyclerMoodCounts) {
            layoutManager = LinearLayoutManager(context)
            adapter = moodAdapter
            isNestedScrollingEnabled = false
        }

        // Click Listeners
        with(binding) {
            btnSelectWeek.setOnClickListener { showWeekSelectionDialog() }
            btnPrevWeek.setOnClickListener { viewModel.onPrevWeekClicked() }
            btnNextWeek.setOnClickListener { viewModel.onNextWeekClicked() }
        }
    }

    private fun setupCharts() {
        setupPieChartConfig()
        setupLineChartConfig()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { handleUiState(it) }
                }
                launch {
                    viewModel.navState.collect { handleNavState(it) }
                }
            }
        }
    }

    private fun handleUiState(state: StatsUiState) {
        when (state) {
            is StatsUiState.Success -> {
                updateStreakInfo(state.stats)
                updatePieChart(state.stats)
                updateLineChart(state.stats)
                moodAdapter.submitList(state.stats.moodCounts)
            }
            is StatsUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is StatsUiState.Loading -> { /* Handle Loading */ }
        }
    }

    private fun handleNavState(navState: NavigationState) {
        binding.btnSelectWeek.text = navState.currentLabel

        updateNavButton(binding.btnPrevWeek, navState.isPrevEnabled)
        updateNavButton(binding.btnNextWeek, navState.isNextEnabled)
    }

    private fun updateNavButton(view: View, isEnabled: Boolean) {
        view.isEnabled = isEnabled
        view.alpha = if (isEnabled) 1.0f else 0.3f
    }

    // --- STREAK LOGIC ---

    private fun updateStreakInfo(stats: WeeklyStats) {
        with(binding) {
            textCurrentStreak.text = stats.currentStreak.toString()
            textLongestStreak.text = stats.longestStreak.toString()
            textTotalEntries.text = "Tổng số mục đã nhập: ${stats.totalEntries}"

            drawStreakNodes(stats.activeDays)
        }
    }

    private fun drawStreakNodes(activeDays: List<Boolean>) {
        val container = binding.streakNodesContainer
        container.removeAllViews()

        val daysLabel = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val density = resources.displayMetrics.density
        val sizePx = (24 * density).toInt()
        val marginPx = (8 * density).toInt()

        activeDays.forEachIndexed { index, isActive ->
            val nodeLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val circleView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    bottomMargin = marginPx
                }
                background = ContextCompat.getDrawable(context,
                    if (isActive) R.drawable.bg_circle_active else R.drawable.bg_circle_inactive
                )
            }

            val textView = TextView(context).apply {
                text = daysLabel[index]
                textSize = 12f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }

            nodeLayout.addView(circleView)
            nodeLayout.addView(textView)
            container.addView(nodeLayout)
        }
    }

    // --- PIE CHART LOGIC ---

    private fun setupPieChartConfig() {
        binding.pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            transparentCircleRadius = 0f
            holeRadius = 60f
            maxAngle = 180f
            rotationAngle = 180f
            setCenterTextOffset(0f, -20f)

            legend.isEnabled = false
            description.isEnabled = false
            setTouchEnabled(false)
            setDrawEntryLabels(false)
        }
    }

    private fun updatePieChart(stats: WeeklyStats) {
        val entries = stats.moodCounts.map { PieEntry(it.count.toFloat(), it.emotion) }

        if (entries.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 2f
            colors = stats.moodCounts.map {
                ContextCompat.getColor(requireContext(), getMoodColorRes(it.emotion))
            }
        }

        val data = PieData(dataSet).apply {
            setDrawValues(false)
        }

        binding.pieChart.data = data
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    // --- LINE CHART LOGIC ---

    private fun setupLineChartConfig() {
        val chart = binding.lineChart

        // General Config
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            isDragEnabled = false
            setTouchEnabled(true)
            minOffset = 0f
            extraLeftOffset = 10f
            extraRightOffset = 20f
            extraBottomOffset = 10f
            extraTopOffset = 10f
        }

        // X Axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            textColor = Color.GRAY
            textSize = 12f
            valueFormatter = IndexAxisValueFormatter(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"))
            granularity = 1f
            axisMinimum = -0.5f
            axisMaximum = 6.5f
        }

        // Y Axis (Left)
        val leftAxis = chart.axisLeft.apply {
            axisMinimum = 0.5f
            axisMaximum = 5.5f
            granularity = 1f
            setLabelCount(5, true)
            setDrawLabels(false)
            setDrawAxisLine(false)
            setDrawGridLines(true) // Required for custom renderer
            setDrawLimitLinesBehindData(true)
        }

        // Disable Right Axis
        chart.axisRight.isEnabled = false

        // Custom Renderer for Background Bands
        setupCustomYAxisRenderer(chart, leftAxis)

        // Click Listener
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val mood = e?.data as? DailyMood
                mood?.let {
                    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    Toast.makeText(context, "${it.emotion} - ${sdf.format(it.date)}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected() {}
        })
    }

    private fun setupCustomYAxisRenderer(chart: com.github.mikephil.charting.charts.LineChart, yAxis: YAxis) {
        val transformer = chart.getTransformer(YAxis.AxisDependency.LEFT)
        val viewPortHandler = chart.viewPortHandler
        val context = requireContext()

        val moodColors = listOf(
            ContextCompat.getColor(context, R.color.emotion_very_dissatisfied),
            ContextCompat.getColor(context, R.color.emotion_dissatisfied),
            ContextCompat.getColor(context, R.color.emotion_neutral),
            ContextCompat.getColor(context, R.color.emotion_satisfied),
            ContextCompat.getColor(context, R.color.emotion_very_satisfied)
        )

        chart.rendererLeftYAxis = object : YAxisRenderer(viewPortHandler, yAxis, transformer) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun renderGridLines(c: Canvas?) {
                if (c == null) return
                val rect = mViewPortHandler.contentRect

                // Draw 5 colored bands
                for (i in 1..5) {
                    val topValue = i + 0.5f
                    val bottomValue = i - 0.5f

                    val pixelTop = mTrans.getPixelForValues(0f, topValue).y.toFloat()
                    val pixelBottom = mTrans.getPixelForValues(0f, bottomValue).y.toFloat()

                    bgPaint.color = moodColors[i - 1]
                    bgPaint.alpha = (255 * 0.2f).toInt() // 20% opacity
                    c.drawRect(rect.left, pixelTop, rect.right, pixelBottom, bgPaint)
                }
            }
        }
    }

    private fun updateLineChart(stats: WeeklyStats) {
        val entries = stats.dailyMoods.mapNotNull { mood ->
            val cal = Calendar.getInstance().apply { time = mood.date }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

            if (index in 0..6) Entry(index.toFloat(), mood.score.toFloat(), mood) else null
        }.sortedBy { it.x }

        if (entries.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        val dataSet = LineDataSet(entries, "Cảm xúc").apply {
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(Color.WHITE)
            circleHoleColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            circleRadius = 6f
            circleHoleRadius = 3f
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            highLightColor = Color.RED
            setDrawHighlightIndicators(true)
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.animateX(1000)
        binding.lineChart.invalidate()
    }

    // --- HELPERS ---

    private fun showWeekSelectionDialog() {
        WeekSelectionBottomSheet(viewModel.firstEntryDate) { selectedWeek ->
            binding.btnSelectWeek.text = selectedWeek.label
            viewModel.loadWeeklyStats(selectedWeek.startDate)
        }.show(childFragmentManager, "WeekSelection")
    }

    private fun getMoodColorRes(emotion: String): Int {
        return when (emotion) {
            "Rất tốt" -> R.color.emotion_very_satisfied
            "Tốt" -> R.color.emotion_satisfied
            "Bình thường" -> R.color.emotion_neutral
            "Tệ" -> R.color.emotion_dissatisfied
            "Rất tệ" -> R.color.emotion_very_dissatisfied
            else -> R.color.emotion_neutral
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}