package com.example.moodpress.feature.stats.data.remote.api

import com.example.moodpress.feature.stats.data.remote.dto.FirstDateResponseDto
import com.example.moodpress.feature.stats.data.remote.dto.WeeklyStatsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface StatsApiService {
    @GET("stats/weekly")
    suspend fun getWeeklyStats(
        @Query("start_date") startDate: String
    ): WeeklyStatsResponseDto

    @GET("stats/monthly")
    suspend fun getMonthlyStats(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("timezone_offset") timezoneOffset: Int
    ): WeeklyStatsResponseDto

    @GET("journal/first-date")
    suspend fun getFirstJournalDate(): FirstDateResponseDto
}