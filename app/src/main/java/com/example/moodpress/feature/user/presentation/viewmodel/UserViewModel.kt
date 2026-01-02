package com.example.moodpress.feature.user.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.user.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class UserUpdateState {
    data object Idle : UserUpdateState()
    data object Loading : UserUpdateState()
    data class Success(val userName: String) : UserUpdateState()
    data class Error(val message: String) : UserUpdateState()
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val updateUsernameUseCase: UpdateUserProfileUseCase
) : ViewModel() {

    private val _updateState = MutableStateFlow<UserUpdateState>(UserUpdateState.Idle)
    val updateState: StateFlow<UserUpdateState> = _updateState

    fun saveProfile(name: String, gender: String, birth: Date) {
        _updateState.value = UserUpdateState.Loading

        viewModelScope.launch {
            try {
                val updatedProfile = updateUsernameUseCase(name, gender, birth)
                _updateState.value =
                    UserUpdateState.Success(updatedProfile.name ?: name)
            } catch (e: Exception) {
                _updateState.value = UserUpdateState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }
}
