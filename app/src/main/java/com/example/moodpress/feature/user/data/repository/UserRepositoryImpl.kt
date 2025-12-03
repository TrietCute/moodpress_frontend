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
        val request = UserProfileUpdateRequestDto(name = name, gender = gender, birth = birth)

        // 1. Gọi API backend
        val responseDto = apiService.updateUserProfile(request)

        // 2. LƯU TÊN LOCAL
        // Sau khi server xác nhận lưu thành công, lưu tên vào SharedPreferences
        responseDto.name?.let {
            sessionManager.saveUserName(it)
            return responseDto.toDomain()
        }

        // 3. Trả về Domain Model
        return responseDto.toDomain()
    }

    override suspend fun linkGoogleAccount(token: String): String {
        // 1. Tạo request DTO
        val request = GoogleLinkRequestDto(google_token = token)

        // 2. Gọi API
        val response = apiService.linkGoogleAccount(request)

        // 3. Trả về ID mới (Server đã xử lý việc gộp dữ liệu)
        return response.new_id
    }
}