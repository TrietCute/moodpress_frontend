package com.example.moodpress.feature.user.data.remote.api

import com.example.moodpress.feature.user.data.remote.dto.GoogleLinkRequestDto
import com.example.moodpress.feature.user.data.remote.dto.GoogleLinkResponseDto
import com.example.moodpress.feature.user.data.remote.dto.UserProfileResponseDto
import com.example.moodpress.feature.user.data.remote.dto.UserProfileUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApiService {

    @PUT("user/profile")
    suspend fun updateUserProfile(
        @Body request: UserProfileUpdateRequestDto
    ): UserProfileResponseDto

    @GET("user/profile")
    suspend fun getUserProfile(): UserProfileResponseDto

    @POST("user/link-google")
    suspend fun linkGoogleAccount(
        @Body request: GoogleLinkRequestDto
    ): GoogleLinkResponseDto
}