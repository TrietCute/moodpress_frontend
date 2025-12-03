package com.example.moodpress.feature.stats.domain.model

import java.util.Date

data class WeeklyStats(
    val moodCounts: List<MoodCount>,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalEntries: Int,
    val allTimeTotal: Int,
    val activeDays: List<Boolean>,
    val dailyMoods: List<DailyMood>
)

data class MoodCount(
    val emotion: String,
    val count: Int,
    val percentage: Float
)

data class DailyMood(
    val date: Date,
    val emotion: String,
    val score: Int // 1-5
)