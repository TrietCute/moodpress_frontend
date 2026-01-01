package com.example.moodpress.feature.chatbot.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ChatRequestDto(
    @SerializedName("message") val message: String,
    val user_info: UserInfo
)

data class UserInfo(
    val name: String,
    val gender: String,
    val birth_date: String
)

data class ChatResponseDto(
    @SerializedName("_id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("sender") val sender: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: Date?
)