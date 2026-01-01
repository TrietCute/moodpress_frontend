package com.example.moodpress.feature.chatbot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.chatbot.data.remote.api.ChatApiService
import com.example.moodpress.feature.chatbot.data.remote.dto.ChatRequestDto
import com.example.moodpress.feature.chatbot.data.repository.ChatRepository
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage
import com.example.moodpress.feature.chatbot.data.remote.dto.UserInfo
import com.example.moodpress.feature.user.data.repository.UserRepository
import dagger.Provides
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        addMessage(ChatMessage("Chào bạn! Mình là trợ lý cảm xúc. Hôm nay bạn thế nào?", isUser = false))
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        addMessage(ChatMessage(content, isUser = true))
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val currentUser = userRepository.getUserProfile()

                val birthDateString = formatDateToIso(currentUser.birth)

                val userInfo = UserInfo(
                    name = currentUser.name ?: "Bạn",
                    gender = currentUser.gender ?: "Bạn",
                    birth_date = birthDateString
                )

                val request = ChatRequestDto(
                    message = content,
                    user_info = userInfo
                )

                val botResponse = chatRepository.sendMessage(request)
                addMessage(botResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                addMessage(ChatMessage("Lỗi: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatDateToIso(date: Date?): String {
        if (date == null) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun addMessage(msg: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(msg)
        _messages.value = currentList
    }

    fun startNewConversation() {
        viewModelScope.launch {
            try {
                _messages.value = emptyList()
                _isLoading.value = true

                // 2. Gọi API Xóa Context trên Server
                //chatRepository.clearHistory()

                addMessage(ChatMessage("Đã làm mới cuộc trò chuyện! Bạn muốn tâm sự gì nào?", isUser = false))

            } catch (e: Exception) {
                addMessage(ChatMessage("Lỗi khi xóa bộ nhớ: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}