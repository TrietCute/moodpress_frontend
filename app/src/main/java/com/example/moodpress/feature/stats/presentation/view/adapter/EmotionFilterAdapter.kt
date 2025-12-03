package com.example.moodpress.feature.stats.presentation.view.adapter

import android.annotation.SuppressLint
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
    val emotionKey: String?,
    val isSelected: Boolean = false
)

class EmotionFilterAdapter(
    private val onFilterSelected: (String?) -> Unit
) : RecyclerView.Adapter<EmotionFilterAdapter.ViewHolder>() {

    // Danh sách cố định các cảm xúc
    private var items = mutableListOf(
        FilterItem("Tất cả", null, true),
        FilterItem("Rất tốt", "Rất tốt"),
        FilterItem("Tốt", "Tốt"),
        FilterItem("Bình thường", "Bình thường"),
        FilterItem("Tệ", "Tệ"),
        FilterItem("Rất tệ", "Rất tệ")
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEmotionFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemEmotionFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem, position: Int) {
            binding.tvLabel.text = item.label

            // Set Icon (Nếu là "Tất cả" thì dùng icon đặc biệt hoặc ẩn đi)
            if (item.emotionKey == null) {
                binding.imgIcon.setImageResource(R.drawable.ic_view_list) // Icon tượng trưng cho All
            } else {
                binding.imgIcon.setImageResource(getMoodIconRes(item.emotionKey))
            }

            // Xử lý trạng thái Selected (Đổi màu nền)
            val context = binding.root.context
            if (item.isSelected) {
                binding.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                binding.tvLabel.setTextColor(Color.WHITE)
                binding.imgIcon.setColorFilter(Color.WHITE)
            } else {
                binding.cardRoot.setCardBackgroundColor(Color.WHITE)
                binding.cardRoot.strokeWidth = 3
                binding.cardRoot.strokeColor = "#E0E0E0".toColorInt()
                binding.tvLabel.setTextColor(Color.BLACK)
                binding.imgIcon.colorFilter = null // Reset màu gốc của icon
            }

            // Click Listener
            binding.root.setOnClickListener {
                // Cập nhật trạng thái chọn
                updateSelection(position)
                // Gửi sự kiện ra ngoài
                onFilterSelected(item.emotionKey)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSelection(selectedIndex: Int) {
        items.forEachIndexed { index, item ->
            items[index] = item.copy(isSelected = (index == selectedIndex))
        }
        notifyDataSetChanged()
    }
}