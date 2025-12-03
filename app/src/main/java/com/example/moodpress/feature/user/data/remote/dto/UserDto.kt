package com.example.moodpress.feature.user.data.remote.dto

import com.example.moodpress.feature.user.domain.model.UserProfile
import java.util.Date

// 1. DTO để GỬI YÊU CẦU (Request)
data class UserProfileUpdateRequestDto(
    val name: String?,
    val gender: String?,
    val birth: Date?

)

// 2. DTO để NHẬN PHẢN HỒI (Response)
data class UserProfileResponseDto(
    val _id: String,
    val name: String?,
    val gender: String?,
    val birth: Date?,
    val email: String?,
    val picture: String?
)

data class GoogleLinkRequestDto(
    val google_token: String
)

data class GoogleLinkResponseDto(
    val message: String,
    val new_id: String, // Đây là Google ID mới
    val email: String?
)

// 3. Hàm Mapper để chuyển đổi DTO (Data) sang Model (Domain)
fun UserProfileResponseDto.toDomain(): UserProfile {
    return UserProfile(
        id = this._id,
        name = this.name,
        gender = this.gender,
        birth = this.birth,
        email = this.email,
        picture = this.picture
    )
}