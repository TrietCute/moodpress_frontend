package com.example.moodpress.feature.stats.presentation.view.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.databinding.DialogWeekSelectionBinding
import com.example.moodpress.feature.stats.domain.model.WeekOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.moodpress.feature.stats.presentation.view.adapter.WeekAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeekSelectionBottomSheet(
    private val firstEntryDate: Date?, // Ngày viết nhật ký đầu tiên
    private val onWeekSelected: (WeekOption) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogWeekSelectionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogWeekSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val weekOptions = generateWeekOptions()

        binding.recyclerWeeks.layoutManager = LinearLayoutManager(context)
        binding.recyclerWeeks.adapter = WeekAdapter(weekOptions) { selected ->
            onWeekSelected(selected)
            dismiss()
        }
    }

    private fun generateWeekOptions(): List<WeekOption> {
        val options = mutableListOf<WeekOption>()
        val sdf = SimpleDateFormat("dd/MM", Locale("vi", "VN")) // Dùng Locale Việt Nam

        // --- OPTION 1: 7 NGÀY GẦN ĐÂY ---
        val today = Calendar.getInstance(Locale("vi", "VN"))
        // Reset giờ về 00:00:00 để so sánh chính xác
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)

        val last7DaysStart = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        options.add(WeekOption("7 ngày gần đây", last7DaysStart.time, true))

        // Nếu chưa có dữ liệu ngày đầu tiên, chỉ trả về Option 1
        if (firstEntryDate == null) return options

        // --- OPTION 2..N: CÁC TUẦN CŨ ---

        // 1. Xác định "Thứ 2 của tuần hiện tại"
        val currentWeekStart = getMondayOfWeek(today)

        // 2. Xác định "Thứ 2 của tuần chứa ngày viết nhật ký đầu tiên"
        val firstDateCal = Calendar.getInstance(Locale("vi", "VN"))
        firstDateCal.time = firstEntryDate
        val startCal = getMondayOfWeek(firstDateCal)

        // 3. Vòng lặp từ tuần đầu tiên -> tuần hiện tại
        val weeklyOptions = mutableListOf<WeekOption>()

        // Clone để chạy vòng lặp
        val loopCal = startCal.clone() as Calendar

        // Chạy cho đến khi vượt quá tuần hiện tại
        // (!after nghĩa là <=, bao gồm cả tuần hiện tại)
        while (!loopCal.after(currentWeekStart)) {
            val start = loopCal.time

            // Ngày cuối tuần là Thứ 2 + 6 ngày (Chủ Nhật)
            val endCal = (loopCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }

            val label = "Tuần: ${sdf.format(start)} - ${sdf.format(endCal.time)}"
            weeklyOptions.add(WeekOption(label, start, false))

            // Tăng thêm 1 tuần (7 ngày)
            loopCal.add(Calendar.DAY_OF_YEAR, 7)
        }

        // Đảo ngược để tuần mới nhất nằm trên cùng
        options.addAll(weeklyOptions.reversed())
        return options
    }

    /**
     * Hàm helper siêu chuẩn để tìm Thứ 2 của tuần chứa ngày 'cal'
     * Bất chấp Locale của máy là Mỹ hay Việt Nam.
     */
    private fun getMondayOfWeek(cal: Calendar): Calendar {
        val newCal = cal.clone() as Calendar
        // Java Calendar: CN=1, T2=2, ..., T7=7
        val dayOfWeek = newCal.get(Calendar.DAY_OF_WEEK)

        // Tính số ngày cần lùi để về Thứ 2
        // Nếu là CN (1) -> Lùi 6 ngày
        // Nếu là T2 (2) -> Lùi 0 ngày
        // Nếu là T3 (3) -> Lùi 1 ngày ...
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

        newCal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        // Reset giờ về 00:00:00
        newCal.set(Calendar.HOUR_OF_DAY, 0)
        newCal.set(Calendar.MINUTE, 0)
        newCal.set(Calendar.SECOND, 0)
        newCal.set(Calendar.MILLISECOND, 0)

        return newCal
    }
}