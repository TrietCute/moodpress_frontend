package com.example.moodpress.feature.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.feature.user.domain.usecase.GetUserProfileUseCase
import com.example.moodpress.feature.user.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Loaded(val profile: UserProfile) : ProfileUiState()
    data object Success : ProfileUiState() // Lưu thành công
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val profile = getUserProfileUseCase()
                _uiState.value = ProfileUiState.Loaded(profile)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Lỗi tải thông tin")
            }
        }
    }

    fun saveProfile(name: String, gender: String?, birth: Date?) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                updateUserProfileUseCase(name, gender, birth)
                _uiState.value = ProfileUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Lỗi lưu thông tin")
            }
        }
    }
}