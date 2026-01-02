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

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.BaseChatViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> {
                val binding = ItemChatUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            else -> { // TYPE_BOT
                val binding = ItemChatBotBinding.inflate(inflater, parent, false)
                BotViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // --- ViewHolders ---

    abstract class BaseChatViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
        abstract fun bind(message: ChatMessage)
    }

    class UserViewHolder(private val binding: ItemChatUserBinding) : BaseChatViewHolder(binding) {
        override fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    class BotViewHolder(private val binding: ItemChatBotBinding) : BaseChatViewHolder(binding) {
        override fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem === newItem || oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}