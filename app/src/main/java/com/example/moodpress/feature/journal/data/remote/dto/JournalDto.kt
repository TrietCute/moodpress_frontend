package com.example.moodpress.feature.journal.data.remote.dto

import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.google.gson.annotations.SerializedName
import java.util.Date

data class AIAnalysisDto(
    val sentiment_score: Float,
    val detected_emotion: String,
    val is_match: Boolean? = true,
    val suggested_emotion: String? = null,
    val advice: String? = null

)

data class JournalEntryResponseDto(
    @SerializedName("_id")
    val id: String,
    val user_id: String,
    val timestamp: Date,
    @SerializedName("emotion_selected")
    val emotion: String,
    val content: String,
    @SerializedName("image_urls") val imageUrls: List<String>? = emptyList(),
    @SerializedName("analysis")
    val analysisDto: AIAnalysisDto?
)

fun JournalEntryResponseDto.toDomain(): JournalEntry {
    return JournalEntry(
        id = this.id,
        content = this.content,
        emotion = this.emotion,
        timestamp = this.timestamp,
        imageUrls = this.imageUrls ?: emptyList(),
        analysis = this.analysisDto?.toDomain()
    )
}

fun AIAnalysisDto.toDomain(): AIAnalysis {
    return AIAnalysis(
        sentimentScore = this.sentiment_score,
        detectedEmotion = this.detected_emotion,
        isMatch = this.is_match ?: true,
        suggestedEmotion = this.suggested_emotion ?: "",
        advice = this.advice ?: ""
    )
}