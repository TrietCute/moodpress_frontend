package com.example.moodpress.feature.home.domain.model

import java.util.Date

data class CalendarDay(
    val date: Date,
    val dayOfMonth: String,
    val isCurrentMonth: Boolean,
    val emotion: String? = null
)