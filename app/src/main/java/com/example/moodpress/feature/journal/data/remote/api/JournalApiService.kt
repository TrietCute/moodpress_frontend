package com.example.moodpress.feature.journal.data.remote.api

import com.example.moodpress.feature.journal.data.remote.dto.AIAnalysisDto
import com.example.moodpress.feature.journal.data.remote.dto.AnalyzeJournalRequestDto
import com.example.moodpress.feature.journal.data.remote.dto.JournalEntryResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface JournalApiService {

    @FormUrlEncoded
    @POST("journal/new")
    suspend fun createJournalEntry(
        @Field("content") content: String,
        @Field("emotion") emotion: String,
        @Field("timestamp") timestamp: String,
        @Field("image_urls") imageUrls: List<String>
    ): JournalEntryResponseDto

    @GET("journal/{entry_id}")
    suspend fun getJournalEntry(@Path("entry_id") id: String): JournalEntryResponseDto

    @FormUrlEncoded
    @PUT("journal/{entry_id}")
    suspend fun updateJournalEntry(
        @Path("entry_id") id: String,
        @Field("content") content: String?,
        @Field("emotion") emotion: String?,
        @Field("timestamp") timestamp: String?,
        @Field("image_urls") imageUrls: List<String>
    ): JournalEntryResponseDto

    @DELETE("journal/{entry_id}")
    suspend fun deleteJournalEntry(@Path("entry_id") id: String)

    @GET("journal/history")
    suspend fun getHistory(@Query("year") year: Int, @Query("month") month: Int): List<JournalEntryResponseDto>

    @POST("journal/analyze")
    suspend fun analyzeJournal(
        @Body request: AnalyzeJournalRequestDto
    ): AIAnalysisDto
}