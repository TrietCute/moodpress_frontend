package com.example.moodpress.feature.home.presentation.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.moodpress.R

class JournalListAdapter(
    private val listener: OnJournalActionsListener
) : ListAdapter<JournalEntry, JournalListAdapter.JournalViewHolder>(JournalDiffCallback()) {

    interface OnJournalActionsListener {
        fun onMoreOptionsClicked(entry: JournalEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)

        holder.binding.buttonOptions.setOnClickListener {
            listener.onMoreOptionsClicked(entry)
        }
    }

    inner class JournalViewHolder(val binding: ItemJournalEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm a, EEE dd/MM", Locale("vi", "VN"))
        private val context = binding.root.context

        fun bind(entry: JournalEntry) {
            binding.journalContent.text = entry.content
            binding.journalTime.text = timeFormat.format(entry.timestamp)

            val iconRes = getEmotionIconRes(entry.emotion)
            binding.emotionIcon.setImageResource(iconRes)
        }

        private fun getEmotionIconRes(emotion: String): Int {
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
}

class JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
    override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
        return oldItem == newItem
    }
}