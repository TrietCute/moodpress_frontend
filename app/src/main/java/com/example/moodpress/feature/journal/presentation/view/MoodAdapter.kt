package com.example.moodpress.feature.journal.presentation.view

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.databinding.ItemEmotionFilterBinding
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt


class MoodAdapter(
    private val onMoodSelected: (MoodItem) -> Unit
) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

    private var items = listOf<MoodItem>()
    private var selectedPosition = -1

    fun submitList(newItems: List<MoodItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectedMood(moodValue: String) {
        val index = items.indexOfFirst { it.value == moodValue }
        if (index != -1 && index != selectedPosition) {
            val oldPos = selectedPosition
            selectedPosition = index
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val binding = ItemEmotionFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    override fun getItemCount() = items.size

    inner class MoodViewHolder(private val binding: ItemEmotionFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MoodItem, isSelected: Boolean) {
            val context = binding.root.context
            val moodColor = ContextCompat.getColor(context, item.colorRes)

            with(binding) {
                imgIcon.setImageResource(item.iconRes)
                tvLabel.text = item.name

                if (isSelected) {
                    // Active State
                    val fadedColor = ColorUtils.setAlphaComponent(moodColor, 40)
                    cardRoot.setCardBackgroundColor(fadedColor)
                    cardRoot.strokeColor = moodColor
                    cardRoot.strokeWidth = 2

                    imgIcon.setColorFilter(moodColor)
                    tvLabel.setTextColor(moodColor)
                    tvLabel.typeface = Typeface.DEFAULT_BOLD
                } else {
                    // Inactive State
                    cardRoot.setCardBackgroundColor(Color.WHITE)
                    cardRoot.strokeColor = "#E0E0E0".toColorInt()
                    cardRoot.strokeWidth = 2 // dpToPx logic here

                    val grayColor = "#757575".toColorInt()
                    imgIcon.setColorFilter(grayColor)
                    tvLabel.setTextColor(grayColor)
                    tvLabel.typeface = Typeface.DEFAULT
                }

                root.setOnClickListener {
                    onMoodSelected(item)
                }
            }
        }
    }
}