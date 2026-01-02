package com.example.moodpress.feature.stats.presentation.view.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemEmotionFilterBinding
import androidx.core.graphics.toColorInt

data class FilterItem(
    val label: String,
    val emotionKey: String?
)

class EmotionFilterAdapter(
    private val onFilterSelected: (String?) -> Unit
) : RecyclerView.Adapter<EmotionFilterAdapter.ViewHolder>() {

    private val items = listOf(
        FilterItem("Tất cả", null),
        FilterItem("Rất tốt", "Rất tốt"),
        FilterItem("Tốt", "Tốt"),
        FilterItem("Bình thường", "Bình thường"),
        FilterItem("Tệ", "Tệ"),
        FilterItem("Rất tệ", "Rất tệ")
    )

    // Theo dõi vị trí đang chọn để tối ưu update (Default: 0 - Tất cả)
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmotionFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemEmotionFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem, isSelected: Boolean) {
            with(binding) {
                tvLabel.text = item.label
                imgIcon.setImageResource(getIconRes(item.emotionKey))

                updateAppearance(isSelected)

                root.setOnClickListener {
                    handleSelection(bindingAdapterPosition, item.emotionKey)
                }
            }
        }

        private fun updateAppearance(isSelected: Boolean) {
            val context = binding.root.context

            if (isSelected) {
                binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                binding.cardRoot.strokeWidth = 0
                binding.tvLabel.setTextColor(Color.WHITE)
                binding.imgIcon.setColorFilter(Color.WHITE)
            } else {
                binding.cardRoot.setCardBackgroundColor(Color.WHITE)
                binding.cardRoot.strokeWidth = 3
                binding.cardRoot.strokeColor = "#E0E0E0".toColorInt()
                binding.tvLabel.setTextColor(Color.BLACK)
                binding.imgIcon.colorFilter = null
            }
        }

        private fun getIconRes(emotionKey: String?): Int {
            return when (emotionKey) {
                null -> R.drawable.ic_view_list
                "Rất tốt" -> R.drawable.ic_emotion_very_satisfied
                "Tốt" -> R.drawable.ic_emotion_satisfied
                "Bình thường" -> R.drawable.ic_emotion_neutral
                "Tệ" -> R.drawable.ic_emotion_dissatisfied
                "Rất tệ" -> R.drawable.ic_emotion_very_dissatisfied
                else -> R.drawable.ic_emotion_neutral
            }
        }
    }

    private fun handleSelection(newPosition: Int, emotionKey: String?) {
        if (newPosition == RecyclerView.NO_POSITION || newPosition == selectedPosition) return

        val previousPosition = selectedPosition
        selectedPosition = newPosition

        // Chỉ update 2 item bị thay đổi thay vì vẽ lại toàn bộ list
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)

        onFilterSelected(emotionKey)
    }
}