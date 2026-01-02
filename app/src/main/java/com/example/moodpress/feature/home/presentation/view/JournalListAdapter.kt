package com.example.moodpress.feature.home.presentation.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemJournalEntryBinding
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.Locale

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
        holder.bind(getItem(position))
    }

    inner class JournalViewHolder(private val binding: ItemJournalEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm a, EEE dd/MM", Locale("vi", "VN"))

        fun bind(entry: JournalEntry) {
            with(binding) {
                journalContent.text = entry.content
                journalTime.text = timeFormat.format(entry.timestamp)
                emotionIcon.setImageResource(getEmotionIconRes(entry.emotion))

                buttonOptions.setOnClickListener {
                    listener.onMoreOptionsClicked(entry)
                }
            }
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