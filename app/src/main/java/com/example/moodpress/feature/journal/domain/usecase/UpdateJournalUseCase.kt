package com.example.moodpress.feature.journal.domain.usecase

import com.example.moodpress.feature.journal.data.repository.JournalRepository
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import java.util.Date
import javax.inject.Inject

class UpdateJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {

    suspend operator fun invoke(
        id: String,
        content: String,
        emotion: String,
        dateTime: Date?,
        imageUrls: List<String>,

    ): JournalEntry {
        if (id.isBlank()) {
            throw IllegalArgumentException("ID nhật ký không được để trống.")
        }
        if (content.isBlank() || content.length < 3) {
            throw IllegalArgumentException("Nội dung phải có ít nhất 3 ký tự.")
        }
        if (emotion.isBlank()) {
            throw IllegalArgumentException("Bạn phải chọn một cảm xúc.")
        }
        return repository.updateJournal(id, content, emotion, dateTime, imageUrls)
    }
}