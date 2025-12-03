package com.example.moodpress.feature.journal.data.repository

import com.example.moodpress.feature.journal.data.remote.api.JournalApiService
import com.example.moodpress.feature.journal.data.remote.dto.AnalyzeJournalRequestDto
import com.example.moodpress.feature.journal.data.remote.dto.toDomain
import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val apiService: JournalApiService
) : JournalRepository {

    override suspend fun saveJournal(
        content: String,
        emotion: String,
        dateTime: Date,
        imageUrls: List<String>
    ): JournalEntry {

        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(dateTime)

        val response = apiService.createJournalEntry(content, emotion, isoDate, imageUrls)
        return response.toDomain()
    }

    override suspend fun getJournalEntry(id: String): JournalEntry {
        return apiService.getJournalEntry(id).toDomain()
    }

    override suspend fun updateJournal(
        id: String,
        content: String,
        emotion: String,
        dateTime: Date?,
        imageUrls: List<String>,
    ): JournalEntry {


        val isoDate = dateTime?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(it)
        }

        val response = apiService.updateJournalEntry(
            id, content, emotion, isoDate, imageUrls
        )
        return response.toDomain()
    }

    override suspend fun deleteJournal(id: String) {
        apiService.deleteJournalEntry(id)
    }

    override suspend fun getJournalHistory(year: Int, month: Int): List<JournalEntry> {
        return apiService.getHistory(year, month).map { it.toDomain() }
    }

    override suspend fun analyzeJournal(
        content: String,
        emotion: String,
        imageUrls: List<String>
    ): AIAnalysis {

        val request = AnalyzeJournalRequestDto(
            content = content,
            emotion = emotion,
            imageUrls = imageUrls
        )

        return apiService.analyzeJournal(request).toDomain()
    }
}