package com.example.moodpress.feature.journal.domain.usecase

import com.example.moodpress.feature.journal.data.repository.JournalRepository
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import javax.inject.Inject

class GetJournalEntryUseCase @Inject constructor(
    private val repository: JournalRepository
) {

    suspend operator fun invoke(id: String): JournalEntry {
        if (id.isBlank()) {
            throw IllegalArgumentException("ID nhật ký không được để trống.")
        }
        return repository.getJournalEntry(id)
    }
}