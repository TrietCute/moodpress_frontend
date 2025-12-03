package com.example.moodpress.feature.chatbot.data.repository

import com.example.moodpress.feature.chatbot.data.remote.api.ChatApiService
import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage
import java.util.Date
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ChatApiService
) : ChatRepository {
    override suspend fun sendMessage(message: String): ChatMessage {
        // 1. Gọi API
        val request = ChatRequestDto(message)
        val response = apiService.sendMessage(request)

        // 2. Map DTO sang Domain Model
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