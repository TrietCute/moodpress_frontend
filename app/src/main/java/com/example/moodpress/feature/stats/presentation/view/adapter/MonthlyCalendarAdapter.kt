package com.example.moodpress.feature.stats.presentation.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemCalendarDayGridBinding

data class MonthDayUiModel(
    val dayValue: String,
    val emotion: String?,
    val isDimmed: Boolean = false
)

class MonthlyCalendarAdapter : ListAdapter<MonthDayUiModel, MonthlyCalendarAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarDayGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCalendarDayGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonthDayUiModel) {
            // 1. Xử lý ô trống (padding đầu tháng)
            if (item.dayValue.isEmpty()) {
                binding.root.visibility = View.INVISIBLE
                return
            }
            binding.root.visibility = View.VISIBLE

            // LUÔN LUÔN HIỂN THỊ SỐ NGÀY
            binding.tvDay.text = item.dayValue
            binding.tvDay.visibility = View.VISIBLE

            // 2. Xử lý hiển thị Icon Cảm xúc
            if (item.emotion != null) {
                // Có nhật ký -> Hiện Icon
                binding.imgMarker.visibility = View.VISIBLE
                val iconRes = getMoodIconRes(item.emotion)
                binding.imgMarker.setImageResource(iconRes)
            } else {
                binding.imgMarker.visibility = View.INVISIBLE
            }

            // 3. Xử lý Bộ lọc (Làm mờ)
            binding.root.alpha = if (item.isDimmed) 0.3f else 1.0f
        }

        private fun getMoodIconRes(emotion: String): Int {
            return when (emotion) {
                "Rất tốt" -> R.drawable.ic_emotion_very_satisfied
                "Tốt" -> R.drawable.ic_emotion_satisfied
                "Bình thường" -> R.drawable.ic_emotion_neutral
                "Tệ" -> R.drawable.ic_emotion_dissatisfied
                "Rất tệ" -> R.drawable.ic_emotion_very_dissatisfied
                else -> R.drawable.ic_emotion_neutral
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MonthDayUiModel>() {
        override fun areItemsTheSame(oldItem: MonthDayUiModel, newItem: MonthDayUiModel): Boolean {
            return oldItem.dayValue == newItem.dayValue
        }
        override fun areContentsTheSame(oldItem: MonthDayUiModel, newItem: MonthDayUiModel): Boolean {
            return oldItem == newItem
        }
    }
}