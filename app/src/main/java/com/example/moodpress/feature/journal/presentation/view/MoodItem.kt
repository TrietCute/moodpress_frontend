package com.example.moodpress.feature.journal.presentation.view

data class MoodItem(
    val value: String,
    val name: String,
    val iconRes: Int,
    val colorRes: Int,
    var isSelected: Boolean = false
)