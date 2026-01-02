package com.example.moodpress.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.core.utils.SessionManager
import com.example.moodpress.feature.user.data.repository.UserRepository
import com.example.moodpress.feature.user.domain.model.UserProfile
import com.example.moodpress.feature.user.domain.usecase.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val linkState: StateFlow<LinkAccountState> = _linkState.asStateFlow()

    init {
        checkCurrentLinkStatus()
    }

    private fun checkCurrentLinkStatus() {
        viewModelScope.launch {
            try {
                val profile = getUserProfileUseCase()
                if (!profile.email.isNullOrEmpty()) {
                    _linkState.value = LinkAccountState.Linked(profile)
                }
            } catch (_: Exception) {
                // Fail silently on init check
            }
        }
    }

    fun linkGoogleAccount(idToken: String) {
        viewModelScope.launch {
            _linkState.value = LinkAccountState.Loading
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