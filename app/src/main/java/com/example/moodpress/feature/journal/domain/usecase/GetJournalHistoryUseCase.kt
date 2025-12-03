package com.example.moodpress.feature.journal.domain.usecase

import com.example.moodpress.feature.journal.data.repository.JournalRepository
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import javax.inject.Inject

class GetJournalHistoryUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(year: Int, month: Int): List<JournalEntry> {
        return repository.getJournalHistory(year, month)
    }
}