package com.example.moodpress.feature.chatbot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodpress.feature.chatbot.data.repository.ChatRepository
import com.example.moodpress.feature.chatbot.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    // Danh sách tin nhắn hiển thị trên màn hình
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Trạng thái loading (khi bot đang gõ)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Thêm tin nhắn chào mừng mặc định
        addMessage(ChatMessage("Chào bạn! Mình là trợ lý cảm xúc. Hôm nay bạn thế nào?", isUser = false))
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // 1. Hiển thị tin nhắn của User ngay lập tức
        addMessage(ChatMessage(content, isUser = true))
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val botResponse = chatRepository.sendMessage(content)
                addMessage(botResponse)
            } catch (e: Exception) {
                e.printStackTrace()
                addMessage(ChatMessage("Lỗi: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
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
                // 1. Xóa UI trước cho mượt
                _messages.value = emptyList()
                _isLoading.value = true // Hiện loading chút cho có hiệu ứng

                // 2. Gọi API Xóa Context trên Server
                //chatRepository.clearHistory()

                // 3. Thêm tin nhắn chào mừng mới
                addMessage(ChatMessage("Đã làm mới cuộc trò chuyện! Bạn muốn tâm sự gì nào?", isUser = false))

            } catch (e: Exception) {
                // Nếu lỗi mạng, báo cho người dùng nhưng vẫn reset UI tạm thời
                addMessage(ChatMessage("Lỗi khi xóa bộ nhớ: ${e.message}", isUser = false))
            } finally {
                _isLoading.value = false
            }
        }
    }
}