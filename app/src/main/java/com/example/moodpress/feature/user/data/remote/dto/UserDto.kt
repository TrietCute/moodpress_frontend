package com.example.moodpress.feature.user.data.remote.dto

import com.example.moodpress.feature.user.domain.model.UserProfile
import java.util.Date

data class UserProfileUpdateRequestDto(
    val name: String?,
    val gender: String?,
    val birth: Date?

)

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
    val new_id: String,
    val email: String?
)

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