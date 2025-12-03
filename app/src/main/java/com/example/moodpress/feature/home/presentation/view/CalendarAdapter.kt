package com.example.moodpress.feature.home.presentation.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.feature.home.domain.model.CalendarDay
import com.example.moodpress.databinding.ItemCalendarDayBinding
import androidx.recyclerview.widget.DiffUtil

class CalendarAdapter(
    private val onDayClicked: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun updateDays(newDays: List<CalendarDay>) {
        // (Đây là cách tối ưu dùng DiffUtil, giống ListAdapter)
        val diffCallback = CalendarDiffCallback(this.days, newDays)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.days = newDays
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day)
        holder.itemView.setOnClickListener {
            if (day.isCurrentMonth) {
                onDayClicked(day)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    inner class CalendarViewHolder(private val binding: ItemCalendarDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        fun bind(day: CalendarDay) {
            binding.dayText.text = day.dayOfMonth

            // 1. Xử lý ngày mờ (không thuộc tháng hiện tại)
            if (day.isCurrentMonth) {
                binding.dayText.alpha = 1.0f // Hiển thị rõ
                binding.dayText.setTextColor(ContextCompat.getColor(context, R.color.black)) // (Màu mặc định)
            } else {
                binding.dayText.alpha = 0.4f // Hiển thị mờ
            }

            // 2. Tô màu nền theo cảm xúc
            val (backgroundColor, textColor) = getEmotionColors(day.emotion)

            binding.dayContainer.setBackgroundColor(backgroundColor)
            binding.dayText.setTextColor(textColor)
        }

        private fun getEmotionColors(emotion: String?): Pair<Int, Int> {
            val defaultTextColor = ContextCompat.getColor(context, R.color.black)
            val whiteTextColor = ContextCompat.getColor(context, R.color.white)

            val backgroundColorRes = when (emotion) {
                "Rất tốt" -> R.color.emotion_very_satisfied
                "Tốt" -> R.color.emotion_satisfied
                "Bình thường" -> R.color.emotion_neutral
                "Tệ" -> R.color.emotion_dissatisfied
                "Rất tệ" -> R.color.emotion_very_dissatisfied
                else -> android.R.color.transparent
            }

            val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)

            val textColor = if (emotion == null) defaultTextColor else whiteTextColor

            return Pair(backgroundColor, textColor)
        }
    }
}

class CalendarDiffCallback(
    private val oldList: List<CalendarDay>,
    private val newList: List<CalendarDay>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // So sánh 2 ngày (chúng ta có thể dựa vào text + isCurrentMonth)
        return oldList[oldItemPosition].dayOfMonth == newList[newItemPosition].dayOfMonth &&
                oldList[oldItemPosition].isCurrentMonth == newList[newItemPosition].isCurrentMonth
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // So sánh nội dung (emotion)
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}