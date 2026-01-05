package com.example.moodpress.feature.chatbot.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.databinding.FragmentChatbotBinding
import com.example.moodpress.feature.chatbot.presentation.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val chatAdapter = ChatAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupInput()
        setupToolbar()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> {
                    viewModel.startNewConversation()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()

        }
    }

    private fun setupRecyclerView() {
        with(binding.recyclerChat) {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Tin nhắn mới nhất nằm dưới cùng
            }
        }
    }

    private fun setupInput() {
        with(binding) {
            // Theo dõi thay đổi text để bật/tắt nút gửi
            edtMessage.doAfterTextChanged { text ->
                updateSendButtonState(text.toString())
            }

            btnSend.setOnClickListener {
                val msg = edtMessage.text.toString()
                if (msg.isNotBlank()) {
                    viewModel.sendMessage(msg)
                    edtMessage.setText("")
                }
            }
        }
    }

    private fun updateSendButtonState(text: String, isLoading: Boolean = false) {
        val hasText = text.isNotBlank()
        val canSend = hasText && !isLoading

        binding.btnSend.isEnabled = canSend
        binding.btnSend.alpha = if (canSend) 1.0f else 0.5f
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Observe Messages
                launch {
                    viewModel.messages.collect { msgs ->
                        chatAdapter.submitList(msgs) {
                            // Scroll xuống cuối khi list cập nhật xong
                            if (msgs.isNotEmpty()) {
                                binding.recyclerChat.scrollToPosition(msgs.size - 1)
                            }
                        }
                    }
                }

                // 2. Observe Loading State
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // Cập nhật trạng thái nút gửi (tránh spam khi đang load)
                        updateSendButtonState(binding.edtMessage.text.toString(), isLoading)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}