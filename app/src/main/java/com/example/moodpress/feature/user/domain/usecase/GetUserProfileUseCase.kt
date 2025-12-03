package com.example.moodpress.feature.user.domain.usecase

import com.example.moodpress.feature.user.data.repository.UserRepository
import com.example.moodpress.feature.user.domain.model.UserProfile
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): UserProfile {
        return repository.getUserProfile()
    }
}