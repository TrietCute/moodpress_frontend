package com.example.moodpress.feature.chatbot.data.repository

import com.example.moodpress.feature.chatbot.data.remote.api.ChatApiService
import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage
import java.util.Date
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService
) : ChatRepository {
    override suspend fun sendMessage(request: ChatRequestDto): ChatMessage {
        val response = apiService.sendMessage(request)
        return ChatMessage(
            content = response.message,
            isUser = false,
            timestamp = response.timestamp ?: java.util.Date()
        )
    }

    override suspend fun clearHistory() {
        val response = apiService.clearChatHistory()
        if (!response.isSuccessful) {
            throw Exception("Không thể xóa lịch sử chat: ${response.code()}")
        }
    }
}