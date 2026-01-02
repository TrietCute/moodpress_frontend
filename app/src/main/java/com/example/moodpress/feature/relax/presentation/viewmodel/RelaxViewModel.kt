package com.example.moodpress.feature.relax.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.relax.domain.model.RelaxSound
import com.example.moodpress.feature.relax.data.remote.api.RelaxApiService // Import Interface API
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RelaxViewModel @Inject constructor(
    private val apiService: RelaxApiService
) : ViewModel() {

    private val _sounds = MutableStateFlow<List<RelaxSound>>(emptyList())
    val sounds: StateFlow<List<RelaxSound>> = _sounds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchSounds()
    }

    fun resetAllSoundsUI() {
        val currentList = _sounds.value.toMutableList()
        currentList.forEach {
            it.isPlaying = false
        }
        _sounds.value = currentList
    }

    private fun fetchSounds() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = apiService.getRelaxSounds()
                _sounds.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}