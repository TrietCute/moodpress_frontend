package com.example.moodpress.feature.chatbot.domain.model

import java.util.Date

data class ChatMessage(
    val content: String,
    val isUser: Boolean, // true = User, false = Bot
    val timestamp: Date = Date()
)