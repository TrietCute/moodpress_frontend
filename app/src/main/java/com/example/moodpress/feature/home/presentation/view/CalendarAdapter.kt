package com.example.moodpress.feature.home.presentation.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemCalendarDayBinding
import com.example.moodpress.feature.home.domain.model.CalendarDay

class CalendarAdapter(
    private val onDayClicked: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun updateDays(newDays: List<CalendarDay>) {
        val diffCallback = CalendarDiffCallback(days, newDays)
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

        fun bind(day: CalendarDay) {
            with(binding) {
                dayText.text = day.dayOfMonth

                // 1. Visual State: Alpha (Opacity) based on month
                dayText.alpha = if (day.isCurrentMonth) 1.0f else 0.4f

                // 2. Visual State: Colors based on emotion
                val (backgroundColor, textColor) = resolveColors(day.emotion)

                dayContainer.setBackgroundColor(backgroundColor)
                dayText.setTextColor(textColor)
            }
        }

        @SuppressLint("ResourceAsColor")
        private fun resolveColors(emotion: String?): Pair<Int, Int> {
            val context = binding.root.context

            if (emotion == null) {
                return android.R.color.transparent to ContextCompat.getColor(context, R.color.black)
            }

            // Emotion state
            val backgroundColorRes = when (emotion) {
                "Rất tốt" -> R.color.emotion_very_satisfied
                "Tốt" -> R.color.emotion_satisfied
                "Bình thường" -> R.color.emotion_neutral
                "Tệ" -> R.color.emotion_dissatisfied
                "Rất tệ" -> R.color.emotion_very_dissatisfied
                else -> android.R.color.transparent
            }

            return ContextCompat.getColor(context, backgroundColorRes) to
                    ContextCompat.getColor(context, R.color.white)
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
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.dayOfMonth == newItem.dayOfMonth &&
                oldItem.isCurrentMonth == newItem.isCurrentMonth
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}