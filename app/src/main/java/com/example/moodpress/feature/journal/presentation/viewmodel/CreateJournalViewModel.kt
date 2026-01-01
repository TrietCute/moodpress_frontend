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

// Trạng thái của việc lưu
sealed class SaveJournalState {
    data object Idle : SaveJournalState()
    data object Loading : SaveJournalState()
    // isSilent = true: Lưu "âm thầm" (sau khi confirm AI), UI sẽ tự thoát
    // isSilent = false: Lưu lần đầu, UI cần check AI Analysis
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
    val saveState: StateFlow<SaveJournalState> = _saveState

    private val _journalData = MutableStateFlow<JournalEntry?>(null)
    val journalData: StateFlow<JournalEntry?> = _journalData

    private val _currentMood = MutableStateFlow<String>("")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    // ID hiện tại, dùng để biết đang Create hay Edit, và lưu lại ID sau khi Create thành công
    var currentJournalId: String? = savedStateHandle["journalId"]
    private val isEditMode: Boolean
        get() = currentJournalId != null

    private val _currentImages = MutableStateFlow<List<Any>>(emptyList())
    val currentImages: StateFlow<List<Any>> = _currentImages

    init {
        // Nếu có ID truyền vào (Edit mode), load dữ liệu
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

    fun updateJournalEmotion(newEmotion: String) {
        _currentMood.value = newEmotion
        // Cập nhật luôn vào object data nếu có
        _journalData.update { it?.copy(emotion = newEmotion) }
    }

    // --- FIX CHÍNH: Hàm Update nhanh dùng cho xác nhận AI ---
    // Hàm này bỏ qua bước xử lý ảnh, chỉ update text/emotion dựa trên ID đã có
    fun quickUpdateEmotion(newEmotion: String) {
        val journalId = currentJournalId ?: return

        // 1. Cập nhật UI state ngay lập tức
        _currentMood.value = newEmotion
        _saveState.value = SaveJournalState.Loading

        viewModelScope.launch {
            try {
                // 2. Lấy dữ liệu hiện tại (Content + Ảnh đã upload xong từ bước trước)
                val currentContent = _journalData.value?.content ?: ""
                // Lúc này _currentImages toàn là String (URL) do đã xử lý ở saveJournal
                // Ép kiểu an toàn để lấy list String
                val currentImageUrls = _currentImages.value.filterIsInstance<String>()

                // 3. Gọi Update UseCase (Nhanh vì không upload ảnh)
                val resultEntry = updateJournalUseCase(
                    id = journalId,
                    content = currentContent,
                    emotion = newEmotion,
                    dateTime = null, // Giữ nguyên ngày giờ cũ
                    imageUrls = currentImageUrls
                )

                // 4. Cập nhật lại data và báo Success với isSilent = true
                _journalData.value = resultEntry
                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = true)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error("Lỗi cập nhật cảm xúc: ${e.message}")
            }
        }
    }

    // --- Hàm Lưu chính (Create/Edit) ---
    fun saveJournal(
        context: Context,
        content: String,
        emotion: String,
        selectedDate: Date,
        isSilent: Boolean // Biến này dùng để điều khiển flow UI
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
                val finalUrls = mutableListOf<String>()
                val newUrisToUpload = mutableListOf<Uri>()

                // Phân loại ảnh
                _currentImages.value.forEach { item ->
                    if (item is String) finalUrls.add(item)
                    else if (item is Uri) newUrisToUpload.add(item)
                }

                // Upload ảnh mới (Tốn thời gian ở đây)
                if (newUrisToUpload.isNotEmpty()) {
                    val uploadJobs = newUrisToUpload.map { uri ->
                        async { uploadImageToCloudinary(context, uri) }
                    }
                    val newUrls = uploadJobs.awaitAll()
                    finalUrls.addAll(newUrls)
                }

                val resultEntry: JournalEntry

                if (isEditMode) {
                    resultEntry = updateJournalUseCase(
                        id = currentJournalId!!,
                        content = content,
                        emotion = finalEmotion,
                        dateTime = null,
                        imageUrls = finalUrls
                    )
                } else {
                    val finalDateTime = if (isToday(selectedDate)) combineDateAndTime(selectedDate) else setNoonTime(selectedDate)

                    resultEntry = saveJournalUseCase(
                        content = content,
                        emotion = finalEmotion,
                        dateTime = finalDateTime,
                        imageUrls = finalUrls
                    )
                }

                // --- FIX QUAN TRỌNG: Cập nhật lại State sau khi lưu thành công ---
                // 1. Lưu lại ID mới tạo (để nếu user bấm đổi mood, ta có ID để update nhanh)
                currentJournalId = resultEntry.id

                // 2. Cập nhật _journalData
                _journalData.value = resultEntry

                // 3. Cập nhật _currentImages thành toàn bộ là URL (String)
                // Để nếu hàm saveJournal có bị gọi lại, nó sẽ KHÔNG upload ảnh lại nữa
                _currentImages.value = resultEntry.imageUrls

                // Trả về kết quả
                _saveState.value = SaveJournalState.Success(resultEntry, isSilent = isSilent)

            } catch (e: Exception) {
                _saveState.value = SaveJournalState.Error(e.message ?: "Lỗi khi lưu nhật ký")
            }
        }
    }

    // --- CÁC HÀM HELPER (Giữ nguyên) ---

    private suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String = suspendCancellableCoroutine { continuation ->
        val preprocessChain = ImagePreprocessChain()
            .loadWith(BitmapDecoder(1080, 1080))
            .saveWith(BitmapEncoder(BitmapEncoder.Format.JPEG, 80))

        MediaManager.get().upload(uri).unsigned("moodpress_android_upload")
            .preprocess(preprocessChain)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) { continuation.resume(resultData["secure_url"] as String) }
                override fun onError(requestId: String, error: ErrorInfo) { continuation.resumeWithException(Exception("Upload thất bại: ${error.description}")) }
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch(context)
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