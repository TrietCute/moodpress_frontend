package com.example.moodpress.feature.journal.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.moodpress.feature.journal.domain.model.JournalEntry
import com.example.moodpress.feature.journal.domain.usecase.AnalyzeJournalUseCase
import com.example.moodpress.feature.journal.domain.usecase.GetJournalEntryUseCase
import com.example.moodpress.feature.journal.domain.usecase.SaveJournalUseCase
import com.example.moodpress.feature.journal.domain.usecase.UpdateJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// Trạng thái của việc lưu
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
    private val analyzeJournalUseCase: AnalyzeJournalUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveJournalState>(SaveJournalState.Idle)
    val saveState: StateFlow<SaveJournalState> = _saveState

    private val _journalData = MutableStateFlow<JournalEntry?>(null)
    val journalData: StateFlow<JournalEntry?> = _journalData
    private var currentJournalId: String? = savedStateHandle["journalId"]
    private val isEditMode: Boolean
        get() = currentJournalId != null

    private val _currentImages = MutableStateFlow<List<Any>>(emptyList())
    val currentImages: StateFlow<List<Any>> = _currentImages
    
    var isServerImageDeleted = false

    init {
        if (isEditMode) {
            loadJournalDetails(currentJournalId!!)
        }
    }

    private fun loadJournalDetails(id: String) {
        viewModelScope.launch {
            try {
                _journalData.value = getJournalEntryUseCase(id)
            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    fun setInitialImages(imageUrls: List<String>) {
        _currentImages.value = imageUrls
    }

    fun addImages(uris: List<Uri>) {
        val currentList = _currentImages.value.toMutableList()
        currentList.addAll(uris)
        _currentImages.value = currentList
    }

    fun removeImageAt(index: Int) {
        val currentList = _currentImages.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _currentImages.value = currentList
        }
    }

    fun saveJournal(
        content: String,
        emotion: String,
        selectedDate: Date
    ) {
        // 1. Validation cơ bản
        if (content.isBlank()) {
            _saveState.value = SaveJournalState.Error("Nội dung không được để trống.")
            return
        }
        if (emotion.isBlank()) {
            _saveState.value = SaveJournalState.Error("Vui lòng chọn một cảm xúc.")
            return
        }

        _saveState.value = SaveJournalState.Loading

        viewModelScope.launch {
            try {
                val finalUrls = mutableListOf<String>()
                val newUrisToUpload = mutableListOf<Uri>()

                // 2. Phân loại ảnh
                _currentImages.value.forEach { item ->
                    if (item is String) {
                        // Ảnh cũ (URL) -> Giữ nguyên
                        finalUrls.add(item)
                    } else if (item is Uri) {
                        // Ảnh mới (Uri) -> Cần upload
                        newUrisToUpload.add(item)
                    }
                }

                // 3. Upload ảnh mới song song (nếu có)
                if (newUrisToUpload.isNotEmpty()) {
                    val uploadJobs = newUrisToUpload.map { uri ->
                        async { uploadImageToCloudinary(uri) }
                    }
                    val newUrls = uploadJobs.awaitAll()
                    finalUrls.addAll(newUrls)
                }

                val resultEntry: JournalEntry

                // 4. Gọi UseCase tương ứng
                if (isEditMode) {
                    resultEntry = updateJournalUseCase(
                        id = currentJournalId!!,
                        content = content,
                        emotion = emotion,
                        dateTime = null, // Gửi null để giữ nguyên thời gian cũ
                        imageUrls = finalUrls
                    )
                } else {
                    // === CREATE ===
                    val finalDateTime = if (isToday(selectedDate)) {
                        combineDateAndTime(selectedDate)
                    } else {
                        setNoonTime(selectedDate)
                    }

                    resultEntry = saveJournalUseCase(
                        content = content,
                        emotion = emotion,
                        dateTime = finalDateTime,
                        imageUrls = finalUrls
                    )

                    currentJournalId = resultEntry.id
                }

                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = false)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi khi lưu nhật ký")
            }
        }
    }

    fun updateJournalEmotion(newEmotion: String, currentContent: String) {

        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    val currentImages = _journalData.value?.imageUrls ?: emptyList()

                    // Gọi UseCase (API)
                    val resultEntry = updateJournalUseCase(
                        id = currentJournalId!!,
                        content = currentContent,
                        emotion = newEmotion,
                        dateTime = null,
                        imageUrls = currentImages
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- CÁC HÀM HELPER ---

    private suspend fun uploadImageToCloudinary(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri).unsigned("moodpress_android_upload").callback(object : UploadCallback {
            override fun onSuccess(requestId: String, resultData: Map<*, *>) { continuation.resume(resultData["secure_url"] as String) }
            override fun onError(requestId: String, error: ErrorInfo) { continuation.resumeWithException(Exception("Upload thất bại: ${error.description}")) }
            override fun onStart(requestId: String) {}
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
        }).dispatch()
    }

    private fun combineDateAndTime(selectedDate: Date): Date {
        val now = Calendar.getInstance()
        val selected = Calendar.getInstance()
        selected.time = selectedDate
        selected.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        selected.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
        selected.set(Calendar.SECOND, now.get(Calendar.SECOND))
        return selected.time
    }

    private fun setNoonTime(selectedDate: Date): Date {
        val selected = Calendar.getInstance()
        selected.time = selectedDate
        selected.set(Calendar.HOUR_OF_DAY, 12)
        selected.set(Calendar.MINUTE, 0)
        selected.set(Calendar.SECOND, 0)
        return selected.time
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance()
        selected.time = date
        return today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
    }

    fun resetState() {
        _saveState.value = SaveJournalState.Idle
    }
}