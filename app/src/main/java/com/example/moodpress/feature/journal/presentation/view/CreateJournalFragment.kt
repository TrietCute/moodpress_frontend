package com.example.moodpress.feature.journal.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.core.ui.ImageViewerDialog
import com.example.moodpress.databinding.FragmentCreateJournalBinding
import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.presentation.viewmodel.CreateJournalViewModel
import com.example.moodpress.feature.journal.presentation.viewmodel.SaveJournalState
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateJournalFragment : Fragment() {

    private var _binding: FragmentCreateJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateJournalViewModel by viewModels()
    private val args: CreateJournalFragmentArgs by navArgs()
    private lateinit var imageAdapter: ImagePreviewAdapter

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    private var selectedDate: Date = Date()
    private var selectedEmotion: String = "Bình thường"
    private lateinit var emotionButtons: List<ImageButton>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageRecyclerView()

        emotionButtons = binding.emotionLayout.children.mapNotNull { it as? ImageButton }.toList()
        selectEmotion(binding.emotionNeutral)

        // 3. Xử lý Arguments (Sửa hoặc Tạo mới)
        val passedDateMillis = args.selectedDate
        val journalId = args.journalId

        if (journalId != null) {
            observeJournalData()
            binding.buttonDatePicker.isEnabled = false
            binding.buttonDatePicker.alpha = 0.5f
        } else if (passedDateMillis != -1L) {
            this.selectedDate = Date(passedDateMillis)
            updateDateButtonText(passedDateMillis)
        } else {
            updateDateButtonText(selectedDate.time)
        }

        setupClickListeners()
        observeSaveState()
        observeImages()
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImagePreviewAdapter(
            // 1. Sự kiện Click Ảnh -> Mở Dialog
            onImageClick = { imageSource ->
                // imageSource có thể là String (URL) hoặc Uri
                ImageViewerDialog.show(childFragmentManager, imageSource)
            },
            // 2. Sự kiện Xóa Ảnh (Giữ nguyên)
            onDeleteClick = { position ->
                viewModel.removeImageAt(position)
            }
        )

        binding.recyclerImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun observeImages() {
        lifecycleScope.launch {
            viewModel.currentImages.collect { images ->
                imageAdapter.submitList(images)
                binding.recyclerImages.isVisible = images.isNotEmpty()
                binding.btnAddImage.isVisible = images.size < 5
            }
        }
    }

    private fun observeJournalData() {
        lifecycleScope.launch {
            viewModel.journalData.collect { entry ->
                if (entry != null) {
                    binding.contentEditText.setText(entry.content)
                    updateDateButtonText(entry.timestamp.time)
                    selectedDate = entry.timestamp

                    val buttonToSelect = emotionButtons.find {
                        it.contentDescription.toString() == entry.emotion
                    }
                    buttonToSelect?.let { selectEmotion(it) }

                    if (entry.imageUrls.isNotEmpty()) {
                        viewModel.setInitialImages(entry.imageUrls)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonDatePicker.setOnClickListener {
            showDatePicker()
        }

        emotionButtons.forEach { button ->
            button.setOnClickListener {
                selectEmotion(button)
            }
        }

        binding.btnAddImage.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.buttonSave.setOnClickListener {
            val content = binding.contentEditText.text.toString()

            viewModel.saveJournal(
                content = content,
                emotion = selectedEmotion,
                selectedDate = selectedDate,
            )
        }
    }

    private fun observeSaveState() {
        lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveJournalState.Success -> {
                        viewModel.resetState()
                        if (state.isSilent) {
                            Toast.makeText(requireContext(), "Đã cập nhật cảm xúc!", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                            return@collect
                        }

                        val analysis = state.entry.analysis
                        if (analysis != null && !analysis.isMatch) {
                            showConsistencyDialog(analysis)
                        } else if (analysis?.advice?.isNotEmpty() == true) {
                            showAdviceDialog(analysis.advice)
                        } else {
                            Toast.makeText(requireContext(), "Đã lưu!", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }

                    is SaveJournalState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }

                    else -> {}
                }
            }
        }
    }

    // --- CÁC HÀM HELPER GIỮ NGUYÊN ---

    private fun showConsistencyDialog(analysis: AIAnalysis) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🤔 AI có một chút băn khoăn...")
            .setMessage("Bạn chọn cảm xúc là '${selectedEmotion}', nhưng AI cảm thấy nội dung lại thiên về '${analysis.suggestedEmotion}'.\n\n" +
                    "Lời nhắn: \"${analysis.advice}\"\n\n" +
                    "Bạn có muốn đổi sang '${analysis.suggestedEmotion}' cho phù hợp không?")
            .setCancelable(false)
            .setPositiveButton("Đổi thành '${analysis.suggestedEmotion}'") { dialog, _ ->
                val currentContent = binding.contentEditText.text.toString()
                viewModel.updateJournalEmotion(analysis.suggestedEmotion, currentContent)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Đã cập nhật cảm xúc!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton("Giữ nguyên") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .setNeutralButton("Xem lại") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Dialog lời khuyên thông thường
    private fun showAdviceDialog(advice: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✨ Góc chia sẻ từ AI")
            .setMessage(advice)
            .setIcon(R.drawable.ic_chat)
            .setPositiveButton("Cảm ơn") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }


    // Hàm helper để update UI nút bấm cảm xúc
    private fun updateEmotionSelection(newEmotion: String) {
        val button = emotionButtons.find { it.contentDescription.toString() == newEmotion }
        button?.let { selectEmotion(it) }
    }


    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { utcMillis ->
            selectedDate = Date(utcMillis)
            updateDateButtonText(utcMillis)
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun updateDateButtonText(millis: Long) {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN"))
        binding.buttonDatePicker.text = sdf.format(Date(millis))
    }

    private fun selectEmotion(selectedButton: ImageButton) {
        selectedEmotion = selectedButton.contentDescription.toString()
        emotionButtons.forEach { button ->
            button.alpha = 0.5f
        }
        selectedButton.alpha = 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}