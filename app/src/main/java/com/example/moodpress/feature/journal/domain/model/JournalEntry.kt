package com.example.moodpress.feature.journal.domain.model

import java.util.Date

data class JournalEntry (
    val id: String,
    val content: String,
    val emotion: String,
    val timestamp: Date,
    val imageUrls: List<String> = emptyList(),
    val analysis: AIAnalysis?
)

data class AIAnalysis(
    val sentimentScore: Float,
    val detectedEmotion: String,
    val isMatch: Boolean,
    val suggestedEmotion: String,
    val advice: String
)
