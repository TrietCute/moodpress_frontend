package com.example.moodpress.feature.journal.domain.usecase

import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.data.repository.JournalRepository
import javax.inject.Inject

class AnalyzeJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {

    suspend operator fun invoke(
        content: String,
        emotion: String,
        imageUrls: List<String>
    ): AIAnalysis {

        // Validation cơ bản trước khi gọi server
        if (content.isBlank()) {
            throw IllegalArgumentException("Nội dung không được để trống để phân tích.")
        }

        return repository.analyzeJournal(content, emotion, imageUrls)
    }
}