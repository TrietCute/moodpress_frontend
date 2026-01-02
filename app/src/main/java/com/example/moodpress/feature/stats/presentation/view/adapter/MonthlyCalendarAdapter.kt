package com.example.moodpress.feature.stats.presentation.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            with(binding) {
                // 1. Handle Empty Cells (Padding days)
                if (item.dayValue.isEmpty()) {
                    root.visibility = View.INVISIBLE
                    return
                }
                root.visibility = View.VISIBLE

                // 2. Set Day Text
                tvDay.text = item.dayValue

                // 3. Set Mood Icon
                if (item.emotion != null) {
                    imgMarker.visibility = View.VISIBLE
                    imgMarker.setImageResource(getMoodIconRes(item.emotion))
                } else {
                    imgMarker.visibility = View.INVISIBLE
                }

                // 4. Handle Dimming Filter
                root.alpha = if (item.isDimmed) 0.3f else 1.0f
            }
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
            // For calendar days, dayValue is unique enough (except for empty padding)
            return oldItem.dayValue == newItem.dayValue
        }

        override fun areContentsTheSame(oldItem: MonthDayUiModel, newItem: MonthDayUiModel): Boolean {
            return oldItem == newItem
        }
    }
}