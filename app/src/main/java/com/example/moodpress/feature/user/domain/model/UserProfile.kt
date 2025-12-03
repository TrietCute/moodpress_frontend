package com.example.moodpress.feature.user.domain.model

import java.util.Date

data class UserProfile (
    val id: String,
    val name: String?,
    val gender: String?,
    val birth: Date?,
    val email: String?,
    val picture: String?
)