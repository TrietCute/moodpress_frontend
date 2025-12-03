package com.example.moodpress.feature.stats.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemMoodStatBinding
import com.example.moodpress.feature.stats.domain.model.MoodCount

class MoodCountAdapter : RecyclerView.Adapter<MoodCountAdapter.ViewHolder>() {

    private var items: List<MoodCount> = emptyList()

    fun submitList(newItems: List<MoodCount>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMoodStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemMoodStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MoodCount) {
            val context = binding.root.context

            // 1. Set Icon & Màu & Tên
            val (iconRes, colorRes) = getMoodResource(item.emotion)

            binding.imgMoodIcon.setImageResource(iconRes)
            binding.tvMoodName.text = item.emotion

            // 2. Set Progress Bar
            binding.progressMood.progress = item.percentage.toInt()
            binding.progressMood.progressTintList =
                ContextCompat.getColorStateList(context, colorRes)

            // 3. Set Text Count
            binding.tvMoodCount.text = "${item.count} (${item.percentage}%)"
        }

        private fun getMoodResource(emotion: String): Pair<Int, Int> {
            return when (emotion) {
                "Rất tốt" -> R.drawable.ic_emotion_very_satisfied to R.color.emotion_very_satisfied
                "Tốt" -> R.drawable.ic_emotion_satisfied to R.color.emotion_satisfied
                "Bình thường" -> R.drawable.ic_emotion_neutral to R.color.emotion_neutral
                "Tệ" -> R.drawable.ic_emotion_dissatisfied to R.color.emotion_dissatisfied
                "Rất tệ" -> R.drawable.ic_emotion_very_dissatisfied to R.color.emotion_very_dissatisfied
                else -> R.drawable.ic_emotion_neutral to R.color.emotion_neutral
            }
        }
    }
}