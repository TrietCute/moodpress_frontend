package com.example.moodpress.feature.stats.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.example.moodpress.feature.stats.domain.model.WeeklyStats
import com.example.moodpress.feature.stats.domain.model.MoodCount
import com.example.moodpress.feature.stats.domain.model.DailyMood
import java.util.Date

data class WeeklyStatsResponseDto(
    @SerializedName("mood_counts") val moodCounts: List<MoodCountDto>,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("total_entries") val totalEntries: Int,
    @SerializedName("all_time_total") val allTimeTotal: Int,
    @SerializedName("active_days_in_week") val activeDaysInWeek: List<Boolean>,
    @SerializedName("daily_moods") val dailyMoods: List<DailyMoodDto>
)

data class MoodCountDto(
    val emotion: String,
    val count: Int,
    val percentage: Float
)

data class DailyMoodDto(
    val date: Date, // Gson (DateTypeAdapter) sẽ tự xử lý yyyy-MM-dd
    val emotion: String,
    val score: Int
)

// --- MAPPERS ---

fun WeeklyStatsResponseDto.toDomain(): WeeklyStats {
    return WeeklyStats(
        moodCounts = this.moodCounts.map { it.toDomain() },
        currentStreak = this.currentStreak,
        longestStreak = this.longestStreak,
        totalEntries = this.totalEntries,
        allTimeTotal = this.allTimeTotal,
        activeDays = this.activeDaysInWeek,
        dailyMoods = this.dailyMoods.map { it.toDomain() }
    )
}

fun MoodCountDto.toDomain() = MoodCount(emotion, count, percentage)
fun DailyMoodDto.toDomain() = DailyMood(date, emotion, score)

data class FirstDateResponseDto(val date: Date?)