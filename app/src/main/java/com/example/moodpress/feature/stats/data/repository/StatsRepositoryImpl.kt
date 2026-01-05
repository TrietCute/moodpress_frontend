package com.example.moodpress.feature.stats.data.repository

import com.example.moodpress.feature.stats.data.remote.api.StatsApiService
import com.example.moodpress.feature.stats.data.remote.dto.toDomain
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val apiService: StatsApiService
) : StatsRepository {

    override suspend fun getWeeklyStats(startDate: String): WeeklyStats {
        val offsetMinutes = getTimeZoneOffset()
        return apiService.getWeeklyStats(startDate, offsetMinutes).toDomain()
    }

    override suspend fun getFirstJournalDate(): Date? {
        return apiService.getFirstJournalDate().date
    }

    override suspend fun getMonthlyStats(startDate: String, endDate: String): WeeklyStats {
        val offsetMinutes = getTimeZoneOffset()
        return apiService.getMonthlyStats(startDate, endDate, offsetMinutes).toDomain()
    }

    private fun getTimeZoneOffset(): Int {
        val timeZone = TimeZone.getDefault()
        val offsetInMillis = timeZone.getOffset(System.currentTimeMillis())
        return offsetInMillis / 1000 / 60
    }
}