package com.example.moodpress.feature.stats.domain.model

import java.util.Date

data class WeekOption(
    val label: String,
    val startDate: Date,
    val isLast7Days: Boolean = false
)