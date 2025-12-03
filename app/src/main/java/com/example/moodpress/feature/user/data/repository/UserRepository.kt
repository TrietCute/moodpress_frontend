package com.example.moodpress.feature.user.data.repository

import com.example.moodpress.feature.user.domain.model.UserProfile
import java.util.Date

interface UserRepository {
    suspend fun updateUserProfile(name: String, gender: String?, birth: Date?): UserProfile
    suspend fun linkGoogleAccount(idToken: String): String
    suspend fun getUserProfile(): UserProfile
}