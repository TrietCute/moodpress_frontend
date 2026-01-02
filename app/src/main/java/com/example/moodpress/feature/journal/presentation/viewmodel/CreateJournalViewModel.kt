package com.example.moodpress.feature.journal.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.preprocess.BitmapDecoder
import com.cloudinary.android.preprocess.BitmapEncoder
import com.cloudinary.android.preprocess.ImagePreprocessChain
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.feature.journal.domain.usecase.GetJournalEntryUseCase
import com.example.moodpress.feature.journal.domain.usecase.SaveJournalUseCase
import com.example.moodpress.feature.journal.domain.usecase.UpdateJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class SaveJournalState {
    data object Idle : SaveJournalState()
    data object Loading : SaveJournalState()
    data class Success(val entry: JournalEntry, val isSilent: Boolean = false) : SaveJournalState()
    data class Error(val message: String) : SaveJournalState()
}

@HiltViewModel
class CreateJournalViewModel @Inject constructor(
    private val saveJournalUseCase: SaveJournalUseCase,
    private val getJournalEntryUseCase: GetJournalEntryUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveJournalState>(SaveJournalState.Idle)
    val saveState: StateFlow<SaveJournalState> = _saveState.asStateFlow()

    private val _journalData = MutableStateFlow<JournalEntry?>(null)
    val journalData: StateFlow<JournalEntry?> = _journalData.asStateFlow()

    private val _currentMood = MutableStateFlow("")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    private val _currentImages = MutableStateFlow<List<Any>>(emptyList())
    val currentImages: StateFlow<List<Any>> = _currentImages.asStateFlow()

    var currentJournalId: String? = savedStateHandle["journalId"]
    private val isEditMode: Boolean get() = currentJournalId != null

    init {
        currentJournalId?.let { loadJournalDetails(it) }
    }

    private fun loadJournalDetails(id: String) {
        viewModelScope.launch {
            try {
                val data = getJournalEntryUseCase(id)
                _journalData.value = data
                _currentMood.value = data.emotion
                setInitialImages(data.imageUrls)
            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    fun setInitialImages(imageUrls: List<String>) {
        if (_currentImages.value.isEmpty()) {
            _currentImages.value = imageUrls
        }
    }

    fun addImages(uris: List<Uri>) {
        _currentImages.update { it + uris }
    }

    fun removeImageAt(index: Int) {
        val currentList = _currentImages.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _currentImages.value = currentList
        }
    }

    fun updateJournalEmotion(newEmotion: String) {
        _currentMood.value = newEmotion
        _journalData.update { it?.copy(emotion = newEmotion) }
    }

    fun quickUpdateEmotion(newEmotion: String) {
        val journalId = currentJournalId ?: return

        _currentMood.value = newEmotion
        _saveState.value = SaveJournalState.Loading

        viewModelScope.launch {
            try {
                val currentContent = _journalData.value?.content ?: ""
                val currentImageUrls = _currentImages.value.filterIsInstance<String>()

                val resultEntry = updateJournalUseCase(
                    id = journalId,
                    content = currentContent,
                    emotion = newEmotion,
                    dateTime = null,
                    imageUrls = currentImageUrls
                )

                _journalData.value = resultEntry
                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = true)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error("Lỗi cập nhật cảm xúc: ${e.message}")
            }
        }
    }

    fun saveJournal(
        context: Context,
        content: String,
        emotion: String,
        selectedDate: Date,
        isSilent: Boolean
    ) {
        if (content.isBlank()) {
            _saveState.value = SaveJournalState.Error("Nội dung không được để trống.")
            return
        }

        val finalEmotion = _currentMood.value.ifBlank { emotion }
        if (finalEmotion.isBlank()) {
            _saveState.value = SaveJournalState.Error("Vui lòng chọn một cảm xúc.")
            return
        }

        _saveState.value = SaveJournalState.Loading

        viewModelScope.launch {
            try {
                // Separate existing URLs from new URIs
                val currentList = _currentImages.value
                val existingUrls = currentList.filterIsInstance<String>().toMutableList()
                val newUris = currentList.filterIsInstance<Uri>()

                // Upload new images
                if (newUris.isNotEmpty()) {
                    val uploadedUrls = newUris.map { uri ->
                        async { uploadImageToCloudinary(context, uri) }
                    }.awaitAll()
                    existingUrls.addAll(uploadedUrls)
                }

                val resultEntry = if (isEditMode) {
                    updateJournalUseCase(
                        id = currentJournalId!!,
                        content = content,
                        emotion = finalEmotion,
                        dateTime = null,
                        imageUrls = existingUrls
                    )
                } else {
                    val finalDateTime = if (isToday(selectedDate)) {
                        combineDateAndTime(selectedDate)
                    } else {
                        setNoonTime(selectedDate)
                    }

                    saveJournalUseCase(
                        content = content,
                        emotion = finalEmotion,
                        dateTime = finalDateTime,
                        imageUrls = existingUrls
                    )
                }

                // Update internal state after successful save
                currentJournalId = resultEntry.id
                _journalData.value = resultEntry
                _currentImages.value = resultEntry.imageUrls // Replace mixed list with pure URL list

                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = isSilent)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi khi lưu nhật ký")
            }
        }
    }

    fun resetState() {
        _saveState.value = SaveJournalState.Idle
    }

    // --- Helpers ---

    private suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String = suspendCancellableCoroutine { cont ->
        val preprocessChain = ImagePreprocessChain()
            .loadWith(BitmapDecoder(1080, 1080))
            .saveWith(BitmapEncoder(BitmapEncoder.Format.JPEG, 80))

        MediaManager.get().upload(uri).unsigned("moodpress_android_upload")
            .preprocess(preprocessChain)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    cont.resume(resultData["secure_url"] as String)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    cont.resumeWithException(Exception("Upload thất bại: ${error.description}"))
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch(context)
    }

    private fun combineDateAndTime(selectedDate: Date): Date {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            set(Calendar.SECOND, now.get(Calendar.SECOND))
        }.time
    }

    private fun setNoonTime(selectedDate: Date): Date {
        return Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { time = date }

        return today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
    }
}