package com.example.moodpress.feature.journal.data.repository

import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import java.util.Date

interface JournalRepository {

    suspend fun saveJournal(
        content: String,
        emotion: String,
        dateTime: Date,
        imageUrls: List<String>
    ): JournalEntry
    suspend fun getJournalEntry(id: String): JournalEntry

    suspend fun updateJournal(
        id: String,
        content: String,
        emotion: String,
        dateTime: Date?,
        imageUrls: List<String>,
    ): JournalEntry

    suspend fun deleteJournal(id: String)

     suspend fun getJournalHistory(year: Int, month: Int): List<JournalEntry>

     suspend fun analyzeJournal(
        content: String,
        emotion: String,
        imageUrls: List<String>
    ): AIAnalysis
}