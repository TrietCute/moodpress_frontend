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
import java.util.TimeZone
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

    // --- STATES ---

    private val _saveState = MutableStateFlow<SaveJournalState>(SaveJournalState.Idle)
    val saveState: StateFlow<SaveJournalState> = _saveState.asStateFlow()

    private val _journalData = MutableStateFlow<JournalEntry?>(null)
    val journalData: StateFlow<JournalEntry?> = _journalData.asStateFlow()

    private val _currentMood = MutableStateFlow("")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    private val _currentImages = MutableStateFlow<List<Any>>(emptyList())
    val currentImages: StateFlow<List<Any>> = _currentImages.asStateFlow()

    private val _entryDate = MutableStateFlow(Date())
    val entryDate: StateFlow<Date> = _entryDate.asStateFlow()

    private var currentJournalId: String? = savedStateHandle["journalId"]
    private val isEditMode: Boolean get() = currentJournalId != null

    init {
        currentJournalId?.let { loadJournalDetails(it) }
        val selectedDateArg = savedStateHandle.get<Long>("selectedDate")
        if (selectedDateArg != null && selectedDateArg != -1L) {
            _entryDate.value = Date(selectedDateArg)
        }
    }

    // --- DATA LOADING ---

    private fun loadJournalDetails(id: String) {
        viewModelScope.launch {
            try {
                val data = getJournalEntryUseCase(id)
                _journalData.value = data
                _currentMood.value = data.emotion
                _entryDate.value = data.timestamp

                if (_currentImages.value.isEmpty()) {
                    _currentImages.value = data.imageUrls
                }
            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    // --- USER ACTIONS ---

    fun setEntryDate(millis: Long) {
        val calendarUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendarUTC.timeInMillis = millis

        val calendarLocal = Calendar.getInstance()
        calendarLocal.timeInMillis = System.currentTimeMillis()
        calendarLocal.set(Calendar.YEAR, calendarUTC.get(Calendar.YEAR))
        calendarLocal.set(Calendar.MONTH, calendarUTC.get(Calendar.MONTH))
        calendarLocal.set(Calendar.DAY_OF_MONTH, calendarUTC.get(Calendar.DAY_OF_MONTH))
        calendarLocal.set(Calendar.HOUR_OF_DAY, 0)
        calendarLocal.set(Calendar.MINUTE, 0)
        calendarLocal.set(Calendar.SECOND, 0)
        _entryDate.value = calendarLocal.time
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
                    dateTime = _entryDate.value,
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
        isSilent: Boolean = false
    ) {
        if (content.isBlank()) {
            _saveState.value = SaveJournalState.Error("Hãy viết gì đó trước khi lưu nhé!")
            return
        }

        val finalEmotion = _currentMood.value.ifBlank { emotion }
        if (finalEmotion.isBlank()) {
            _saveState.value = SaveJournalState.Error("Bạn đang cảm thấy thế nào? Hãy chọn một cảm xúc.")
            return
        }

        _saveState.value = SaveJournalState.Loading

        viewModelScope.launch {
            try {
                val currentList = _currentImages.value
                val existingUrls = currentList.filterIsInstance<String>().toMutableList()
                val newUris = currentList.filterIsInstance<Uri>()

                if (newUris.isNotEmpty()) {
                    val uploadedUrls = newUris.map { uri ->
                        async { uploadImageToCloudinary(context, uri) }
                    }.awaitAll()
                    existingUrls.addAll(uploadedUrls)
                }

                val finalDateTime = combineDateWithLogic(_entryDate.value)

                val resultEntry = if (isEditMode) {
                    updateJournalUseCase(
                        id = currentJournalId!!,
                        content = content,
                        emotion = finalEmotion,
                        dateTime = finalDateTime,
                        imageUrls = existingUrls
                    )
                } else {
                    saveJournalUseCase(
                        content = content,
                        emotion = finalEmotion,
                        dateTime = finalDateTime,
                        imageUrls = existingUrls
                    )
                }

                currentJournalId = resultEntry.id
                _journalData.value = resultEntry
                _currentImages.value = resultEntry.imageUrls

                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = isSilent)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi khi lưu nhật ký")
            }
        }
    }

    private fun combineDateWithLogic(selectedDate: Date): Date {
        val now = Calendar.getInstance()
        val selectedCal = Calendar.getInstance()
        selectedCal.time = selectedDate

        val isToday = now.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

        return if (isToday) {
            selectedCal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
            selectedCal.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            selectedCal.set(Calendar.SECOND, now.get(Calendar.SECOND))
            selectedCal.time
        } else {
            selectedCal.set(Calendar.HOUR_OF_DAY, 12)
            selectedCal.set(Calendar.MINUTE, 0)
            selectedCal.set(Calendar.SECOND, 0)
            selectedCal.time
        }
    }

    fun resetState() {
        _saveState.value = SaveJournalState.Idle
    }

    // --- HELPERS ---

    private suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String = suspendCancellableCoroutine { cont ->
        val preprocessChain = ImagePreprocessChain()
            .loadWith(BitmapDecoder(1080, 1080))
            .saveWith(BitmapEncoder(BitmapEncoder.Format.JPEG, 80))

        MediaManager.get().upload(uri).unsigned("moodpress_android_upload")
            .preprocess(preprocessChain)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    if (url != null) {
                        cont.resume(url)
                    } else {
                        cont.resumeWithException(Exception("Không lấy được URL ảnh"))
                    }
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    cont.resumeWithException(Exception("Upload thất bại: ${error.description}"))
                }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch(context)
    }

}