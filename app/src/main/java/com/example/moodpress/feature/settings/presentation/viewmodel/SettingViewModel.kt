package com.example.moodpress.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.feature.user.data.repository.UserRepository
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.feature.user.domain.usecase.GetUserProfileUseCase
// (Giả sử bạn đã thêm hàm linkGoogleAccount vào UserRepository)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LinkAccountState {
    data object Idle : LinkAccountState()
    data object Loading : LinkAccountState()
    data class Success(val profile: UserProfile) : LinkAccountState()
    data class Error(val message: String) : LinkAccountState()
    data class Linked(val profile: UserProfile) : LinkAccountState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _linkState = MutableStateFlow<LinkAccountState>(LinkAccountState.Idle)
    val linkState: StateFlow<LinkAccountState> = _linkState

    init {
        checkCurrentLinkStatus()
    }

    private fun checkCurrentLinkStatus() {
        viewModelScope.launch {
            try {
                val profile = getUserProfileUseCase()
                // Nếu profile có email, nghĩa là đã liên kết Google
                if (!profile.email.isNullOrEmpty()) {
                    _linkState.value = LinkAccountState.Linked(profile)
                }
            } catch (e: Exception) {
                // Bỏ qua lỗi
            }
        }
    }

    fun linkGoogleAccount(idToken: String) {
        _linkState.value = LinkAccountState.Loading

        viewModelScope.launch {
            try {
                val newUserId = userRepository.linkGoogleAccount(idToken)
                sessionManager.saveUserId(newUserId)
                val profile = getUserProfileUseCase()
                _linkState.value = LinkAccountState.Success(profile)
            } catch (e: Exception) {
                _linkState.value = LinkAccountState.Error(e.message ?: "Lỗi liên kết")
            }
        }
    }
}