package com.example.moodpress.feature.relax.domain.model

import com.google.gson.annotations.SerializedName

data class RelaxSound(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("audio_url") val audioUrl: String,
    @SerializedName("is_premium") val isPremium: Boolean = false,

    // Các trường dùng cho UI (không từ API)
    var isPlaying: Boolean = false,
    var volume: Float = 0.5f
)