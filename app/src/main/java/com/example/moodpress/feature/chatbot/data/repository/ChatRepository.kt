package com.example.moodpress.feature.chatbot.data.repository

import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequestDto): ChatMessage
    suspend fun clearHistory()
}