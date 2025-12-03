package com.example.moodpress.feature.stats.presentation.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentWeeklyStatsBinding
import com.example.moodpress.feature.stats.domain.model.DailyMood
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.presentation.view.adapter.MoodCountAdapter
import com.example.moodpress.feature.stats.presentation.viewmodel.StatsUiState
import com.example.moodpress.feature.stats.presentation.viewmodel.WeeklyStatsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.createBitmap
import com.example.moodpress.feature.stats.presentation.view.bottomsheet.WeekSelectionBottomSheet

@AndroidEntryPoint
class WeeklyStatsFragment : Fragment() {

    private var _binding: FragmentWeeklyStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeeklyStatsViewModel by viewModels()
    private lateinit var moodAdapter: MoodCountAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeeklyStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupMoodRecycler()
        setupPieChartConfig()
        setupLineChartConfig()
        observeViewModel()
        binding.btnSelectWeek.setOnClickListener {
            showWeekSelectionDialog()
        }
    }

    private fun setupClickListeners() {
        // 1. Nút Chọn Tuần (Mở BottomSheet)
        binding.btnSelectWeek.setOnClickListener {
            showWeekSelectionDialog()
        }

        // 2. Nút Trái
        binding.btnPrevWeek.setOnClickListener {
            viewModel.onPrevWeekClicked()
        }

        // 3. Nút Phải
        binding.btnNextWeek.setOnClickListener {
            viewModel.onNextWeekClicked()
        }
    }

    private fun showWeekSelectionDialog() {
        val firstDate = viewModel.firstEntryDate

        val dialog = WeekSelectionBottomSheet(firstDate) { selectedWeek ->
            // 1. Cập nhật text nút bấm
            binding.btnSelectWeek.text = selectedWeek.label
            // 2. Gọi ViewModel tải dữ liệu
            viewModel.loadWeeklyStats(selectedWeek.startDate)
        }
        dialog.show(childFragmentManager, "WeekSelection")
    }

    private fun setupMoodRecycler() {
        moodAdapter = MoodCountAdapter()
        binding.recyclerMoodCounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = moodAdapter
            isNestedScrollingEnabled = false // Để cuộn mượt trong ScrollView
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is StatsUiState.Success -> {
                        // Cập nhật UI
                        updateStreakInfo(state.stats)
                        updatePieChart(state.stats)
                        updateLineChart(state.stats)
                        moodAdapter.submitList(state.stats.moodCounts)
                    }
                    is StatsUiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is StatsUiState.Loading -> { }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.navState.collect { navState ->
                // Cập nhật Label
                binding.btnSelectWeek.text = navState.currentLabel

                // Cập nhật nút Prev
                binding.btnPrevWeek.isEnabled = navState.isPrevEnabled
                binding.btnPrevWeek.alpha = if (navState.isPrevEnabled) 1.0f else 0.3f

                // Cập nhật nút Next
                binding.btnNextWeek.isEnabled = navState.isNextEnabled
                binding.btnNextWeek.alpha = if (navState.isNextEnabled) 1.0f else 0.3f
            }
        }
    }

    // --- 1. LOGIC STREAK (CHUỖI NGÀY) ---
    private fun updateStreakInfo(stats: WeeklyStats) {
        binding.textCurrentStreak.text = stats.currentStreak.toString()
        binding.textLongestStreak.text = stats.longestStreak.toString()
        binding.textTotalEntries.text = "Tổng số mục đã nhập: ${stats.totalEntries}"

        // Vẽ 7 node tròn (T2 -> CN)
        binding.streakNodesContainer.removeAllViews()

        val daysLabel = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

        stats.activeDays.forEachIndexed { index, isActive ->
            // Tạo layout con bằng code
            val nodeLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Hình tròn
            val circleView = View(context).apply {
                val sizePx = (24 * resources.displayMetrics.density).toInt()
                background = ContextCompat.getDrawable(context,
                    if (isActive) R.drawable.bg_circle_active else R.drawable.bg_circle_inactive
                )
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt() // Margin bottom 8dp
                }
            }

            val textView = TextView(context).apply {
                text = daysLabel[index]
                textSize = 12f
                setTextColor(Color.GRAY)
                gravity = android.view.Gravity.CENTER
            }

            nodeLayout.addView(circleView)
            nodeLayout.addView(textView)
            binding.streakNodesContainer.addView(nodeLayout)
        }
    }

    // --- 2. LOGIC PIE CHART (BÁN NGUYỆT) ---
    private fun setupPieChartConfig() {
        binding.pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            transparentCircleRadius = 0f
            holeRadius = 60f // Lỗ rỗng ở giữa
            maxAngle = 180f // Bán nguyệt
            rotationAngle = 180f // Xoay để cong lên trên
            setCenterTextOffset(0f, -20f)

            legend.isEnabled = false
            description.isEnabled = false
            setTouchEnabled(false)
            setDrawEntryLabels(false) // Không hiện chữ trên biểu đồ
        }
    }

    private fun updatePieChart(stats: WeeklyStats) {
        val entries = stats.moodCounts.map {
            PieEntry(it.count.toFloat(), it.emotion)
        }

        if (entries.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.sliceSpace = 2f

        // Map màu
        val colors = stats.moodCounts.map {
            ContextCompat.getColor(requireContext(), getMoodColorRes(it.emotion))
        }
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setDrawValues(false)

        binding.pieChart.data = data
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    // --- 3. LOGIC LINE CHART (BIỂU ĐỒ ĐƯỜNG) ---
    private fun setupLineChartConfig() {
        val context = requireContext()

        // 1. Cấu hình chung cho biểu đồ
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            isDragEnabled = false
            setTouchEnabled(true)

            // Tắt lề thừa (Vì Icon đã nằm ở Layout XML bên ngoài)
            minOffset = 0f
            extraLeftOffset = 10f
            extraRightOffset = 20f
            extraBottomOffset = 10f
            extraTopOffset = 10f

            // Quan trọng: Vẽ màu nền nằm DƯỚI đường dữ liệu
            axisLeft.setDrawLimitLinesBehindData(true)
        }

        // 2. Cấu hình Trục X
        binding.lineChart.xAxis.apply {
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

        // 3. Cấu hình Trục Y (Trái)
        val leftAxis = binding.lineChart.axisLeft
        leftAxis.apply {
            axisMinimum = 0.5f
            axisMaximum = 5.5f
            granularity = 1f
            setLabelCount(5, true)
            setDrawLabels(false)
            setDrawAxisLine(false)

            // BẮT BUỘC PHẢI BẬT GridLines ĐỂ KÍCH HOẠT HÀM renderGridLines
            setDrawGridLines(true)
        }

        // --- 4. LOGIC TÔ MÀU NỀN (ANONYMOUS CLASS) ---
        val transformer = binding.lineChart.getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT)
        val viewPortHandler = binding.lineChart.viewPortHandler

        binding.lineChart.rendererLeftYAxis = object : com.github.mikephil.charting.renderer.YAxisRenderer(viewPortHandler, leftAxis, transformer) {

            // Chuẩn bị bút vẽ
            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
            }

            // Danh sách màu
            val moodColors = listOf(
                ContextCompat.getColor(context, R.color.emotion_very_dissatisfied),
                ContextCompat.getColor(context, R.color.emotion_dissatisfied),
                ContextCompat.getColor(context, R.color.emotion_neutral),
                ContextCompat.getColor(context, R.color.emotion_satisfied),
                ContextCompat.getColor(context, R.color.emotion_very_satisfied)
            )

            override fun renderGridLines(c: android.graphics.Canvas?) {
                if (c == null) return

                // Lấy vùng hiển thị của biểu đồ
                val rect = mViewPortHandler.contentRect

                // Vẽ 5 dải màu
                for (i in 1..5) {
                    val value = i.toFloat()
                    val topValue = value + 0.5f
                    val bottomValue = value - 0.5f

                    val pixelTop = mTrans.getPixelForValues(0f, topValue).y.toFloat()
                    val pixelBottom = mTrans.getPixelForValues(0f, bottomValue).y.toFloat()

                    // Thiết lập màu và độ trong suốt
                    bgPaint.color = moodColors[i - 1]
                    bgPaint.alpha = (255 * 0.2f).toInt() // Độ mờ 20%
                    c.drawRect(rect.left, pixelTop, rect.right, pixelBottom, bgPaint)
                }
            }
        }

        // 5. Tắt trục Y phải & Sự kiện Click
        binding.lineChart.axisRight.isEnabled = false
        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
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

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    private fun updateLineChart(stats: WeeklyStats) {
        val entries = mutableListOf<Entry>()

        stats.dailyMoods.forEach { mood ->
            val cal = java.util.Calendar.getInstance()
            cal.time = mood.date
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            // Convert CN(1)..T7(7) -> T2(0)..CN(6)
            val index = if(dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - 2

            if (index in 0..6) {
                entries.add(Entry(index.toFloat(), mood.score.toFloat(), mood))
            }
        }

        entries.sortBy { it.x }

        if (entries.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        val lineDataSet = LineDataSet(entries, "Cảm xúc").apply {
            // --- THIẾT KẾ ĐƯỜNG VẼ (Sắc nét hơn) ---

            // Dùng màu ĐEN hoặc XÁM ĐẬM cho đường vẽ để nổi bật trên nền màu
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            lineWidth = 3f // Đường dày hơn

            // Thiết kế Điểm (Mốc)
            setDrawCircles(true)
            setCircleColor(Color.WHITE) // Viền tròn trắng
            circleHoleColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary) // Lõi tròn màu chính
            circleRadius = 6f // Điểm to rõ ràng
            circleHoleRadius = 3f

            // Kiểu đường: LINEAR (Thẳng) để thấy rõ sự lên xuống gắt
            mode = LineDataSet.Mode.LINEAR
            // Hoặc dùng HORIZONTAL_BEZIER nếu muốn cong nhẹ nhưng vẫn rõ bậc
            // mode = LineDataSet.Mode.HORIZONTAL_BEZIER

            // Hiển thị giá trị trên điểm (Tùy chọn)
            setDrawValues(false)

            // Hiệu ứng Highlight khi click
            highLightColor = Color.RED
            setDrawHighlightIndicators(true)
        }

        binding.lineChart.data = LineData(lineDataSet)

        // Animation khi hiển thị (Vẽ từ trái sang phải)
        binding.lineChart.animateX(1000)
        binding.lineChart.invalidate()
    }

    // Helper màu sắc
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

    private fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
        if (this is android.graphics.drawable.BitmapDrawable) {
            return this.bitmap
        }
        val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}