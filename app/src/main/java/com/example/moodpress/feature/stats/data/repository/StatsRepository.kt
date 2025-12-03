package com.example.moodpress.feature.stats.data.repository

import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import java.util.Date

interface StatsRepository {
    suspend fun getWeeklyStats(startDate: String): WeeklyStats
    suspend fun getMonthlyStats(startDate: String, endDate: String): WeeklyStats
    suspend fun getFirstJournalDate(): Date?
}