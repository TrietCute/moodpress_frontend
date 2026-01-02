package com.example.moodpress.feature.chatbot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.data.remote.dto.UserInfo
import com.example.moodpress.feature.chatbot.data.repository.ChatRepository
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage
import com.example.moodpress.feature.user.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Cache formatter để tránh khởi tạo nhiều lần
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    init {
        addMessage(ChatMessage("Chào bạn! Mình là trợ lý cảm xúc. Hôm nay bạn thế nào?", isUser = false))
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        addMessage(ChatMessage(content, isUser = true))
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Chuẩn bị dữ liệu User Info
                val currentUser = userRepository.getUserProfile()
                val userInfo = UserInfo(
                    name = currentUser.name ?: "Bạn",
                    gender = currentUser.gender ?: "Bạn",
                    birth_date = formatDateToIso(currentUser.birth)
                )

                val request = ChatRequestDto(
                    message = content,
                    user_info = userInfo
                )

                // 2. Gọi API
                val botResponse = chatRepository.sendMessage(request)
                addMessage(botResponse)

            } catch (e: Exception) {
                addMessage(ChatMessage("Lỗi kết nối: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Reset UI
                _messages.value = emptyList()

                // TODO: Nếu sau này có API clear context phía server thì gọi ở đây
                // chatRepository.clearHistory()

                addMessage(ChatMessage("Đã làm mới cuộc trò chuyện! Bạn muốn tâm sự gì nào?", isUser = false))
            } catch (e: Exception) {
                addMessage(ChatMessage("Có lỗi xảy ra: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addMessage(msg: ChatMessage) {
        // Cách update list trong StateFlow an toàn và ngắn gọn
        _messages.update { currentList ->
            currentList + msg
        }
    }

    private fun formatDateToIso(date: Date?): String {
        return if (date != null) {
            try {
                isoDateFormat.format(date)
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }
}