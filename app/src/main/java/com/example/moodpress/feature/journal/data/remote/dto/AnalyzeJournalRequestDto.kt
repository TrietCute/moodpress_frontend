package com.example.moodpress.feature.journal.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnalyzeJournalRequestDto(
    val content: String,
    val emotion: String,

    @SerializedName("image_urls")
    val imageUrls: List<String> = emptyList()
)