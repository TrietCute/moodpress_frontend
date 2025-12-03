package com.example.moodpress.feature.chatbot.presentation.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.moodpress.databinding.ItemChatBotBinding
import com.example.moodpress.databinding.ItemChatUserBinding
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        // Kiểm tra người gửi để chọn Layout
        return if (getItem(position).isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val binding = ItemChatUserBinding.inflate(inflater, parent, false)
            UserViewHolder(binding)
        } else {
            val binding = ItemChatBotBinding.inflate(inflater, parent, false)
            BotViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Lớp cha trừu tượng
    abstract class ChatViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(message: ChatMessage)
    }

    // ViewHolder cho User
    inner class UserViewHolder(private val binding: ItemChatUserBinding) : ChatViewHolder(binding) {
        override fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    // ViewHolder cho Bot
    inner class BotViewHolder(private val binding: ItemChatBotBinding) : ChatViewHolder(binding) {
        override fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            // Giả sử ChatMessage có ID, nếu không có thì so sánh content + time
            return oldItem == newItem
        }
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}