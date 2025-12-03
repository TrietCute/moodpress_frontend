package com.example.moodpress.feature.chatbot.data.repository

import com.example.moodpress.feature.chatbot.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(message: String): ChatMessage
    suspend fun clearHistory()
}