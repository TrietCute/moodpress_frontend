package com.example.moodpress.feature.user.domain.usecase

import com.example.moodpress.feature.user.data.repository.UserRepository
import com.example.moodpress.feature.user.domain.model.UserProfile
import java.util.Date
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(name: String, gender: String?, birth: Date?): UserProfile {
        if (name.isBlank()) {
            throw IllegalArgumentException("Tên không được để trống.")
        }
        return repository.updateUserProfile(name, gender, birth)
    }
}