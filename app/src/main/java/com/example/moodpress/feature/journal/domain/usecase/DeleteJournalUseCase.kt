package com.example.moodpress.feature.journal.domain.usecase

import com.example.moodpress.feature.journal.data.repository.JournalRepository
import javax.inject.Inject

class DeleteJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {

    suspend operator fun invoke(id: String) {
        if (id.isBlank()) {
            throw IllegalArgumentException("ID nhật ký không được để trống.")
        }
        repository.deleteJournal(id)
    }
}