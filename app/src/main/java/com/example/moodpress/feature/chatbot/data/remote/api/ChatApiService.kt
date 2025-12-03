package com.example.moodpress.feature.chatbot.data.remote.api

import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.data.remote.dto.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface ChatApiService {
    @POST("chat/send")
    suspend fun sendMessage(@Body request: ChatRequestDto): ChatResponseDto

    @DELETE("chat/history")
    suspend fun clearChatHistory(): retrofit2.Response<Unit>
}