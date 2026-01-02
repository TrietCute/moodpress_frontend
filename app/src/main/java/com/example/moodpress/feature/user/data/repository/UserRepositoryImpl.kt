package com.example.moodpress.feature.user.data.repository

import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.feature.user.data.remote.api.UserApiService
import com.example.moodpress.feature.user.data.remote.dto.GoogleLinkRequestDto
import com.example.moodpress.feature.user.data.remote.dto.UserProfileUpdateRequestDto
import com.example.moodpress.feature.user.data.remote.dto.toDomain
import com.example.moodpress.feature.user.domain.model.UserProfile
import java.util.Date
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val apiService: UserApiService,
    private val sessionManager: SessionManager
) : UserRepository {

    override suspend fun getUserProfile(): UserProfile {
        return apiService.getUserProfile().toDomain()
    }

    override suspend fun updateUserProfile(name: String, gender: String?, birth: Date?): UserProfile {
        val request = UserProfileUpdateRequestDto(
            name = name,
            gender = gender,
            birth = birth
        )

        val responseDto = apiService.updateUserProfile(request)

        // Update local session immediately if name is present
        responseDto.name?.let {
            sessionManager.saveUserName(it)
        }

        return responseDto.toDomain()
    }

    override suspend fun linkGoogleAccount(token: String): String {
        val request = GoogleLinkRequestDto(google_token = token)
        val response = apiService.linkGoogleAccount(request)

        return response.new_id
    }
}