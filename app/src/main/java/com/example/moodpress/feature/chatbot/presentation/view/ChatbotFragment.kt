package com.example.moodpress.feature.chatbot.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
    private lateinit var chatAdapter: ChatAdapter

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
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInput() {
        binding.edtMessage.addTextChangedListener { text ->
            val isEnabled = !text.isNullOrBlank()
            binding.btnSend.isEnabled = isEnabled
            binding.btnSend.alpha = if (isEnabled) 1.0f else 0.5f
        }

        binding.btnSend.setOnClickListener {
            val msg = binding.edtMessage.text.toString()
            viewModel.sendMessage(msg)
            binding.edtMessage.setText("") // Xóa ô nhập
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collect { msgs ->
                chatAdapter.submitList(msgs)
                if (msgs.isNotEmpty()) {
                    binding.recyclerChat.smoothScrollToPosition(msgs.size - 1)
                }
            }
        }

        // (Tùy chọn) Hiển thị trạng thái "Bot đang gõ..."
        lifecycleScope.launch {
            viewModel.isLoading.collect {
                // Bạn có thể thêm một ProgressBar nhỏ hoặc text "Đang nhập..." vào UI
                // binding.progressBar.isVisible = loading
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}