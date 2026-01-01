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
import com.example.moodpress.core.ui.showLoading
import com.example.moodpress.core.ui.hideLoading

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
    // Không cần biến selectedEmotion cục bộ nữa, dùng trực tiếp từ viewModel hoặc binding state
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

        // Mặc định chọn mood đầu tiên nếu chưa có
        if (viewModel.currentMood.value.isBlank()) {
            // Giả sử binding.emotionNeutral là mood mặc định "Bình thường"
            viewModel.updateJournalEmotion(binding.emotionNeutral.contentDescription.toString())
        }

        val passedDateMillis = args.selectedDate
        val journalId = args.journalId

        if (journalId != null) {
            // Edit mode: data sẽ được load trong ViewModel init
            binding.buttonDatePicker.isEnabled = false
            binding.buttonDatePicker.alpha = 0.5f
        } else if (passedDateMillis != -1L) {
            this.selectedDate = Date(passedDateMillis)
            updateDateButtonText(passedDateMillis)
        } else {
            updateDateButtonText(selectedDate.time)
        }

        setupClickListeners()
        observeJournalData()
        observeSaveState()
        observeImages()
        observeCurrentMood() // Thêm hàm quan sát Mood
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImagePreviewAdapter(
            onImageClick = { imageSource ->
                ImageViewerDialog.show(childFragmentManager, imageSource)
            },
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

    // Quan sát Mood để cập nhật UI Button
    private fun observeCurrentMood() {
        lifecycleScope.launch {
            viewModel.currentMood.collect { mood ->
                val buttonToSelect = emotionButtons.find {
                    it.contentDescription.toString() == mood
                }
                buttonToSelect?.let { updateEmotionUI(it) }
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

                    // Mood và Image đã được set trong ViewModel.loadJournalDetails
                    // UI sẽ tự cập nhật thông qua observeCurrentMood và observeImages
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
                // Gọi ViewModel để update state
                viewModel.updateJournalEmotion(button.contentDescription.toString())
            }
        }

        binding.btnAddImage.setOnClickListener {
            pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.buttonSave.setOnClickListener {
            performSave()
        }
    }


    private fun performSave(isSilent: Boolean = false) {
        val content = binding.contentEditText.text.toString()
        // Lấy mood trực tiếp từ ViewModel (nguồn chuẩn nhất)
        val currentMood = viewModel.currentMood.value

        viewModel.saveJournal(
            context = requireContext().applicationContext,
            content = content,
            emotion = currentMood,
            selectedDate = selectedDate,
            isSilent = isSilent
        )
    }

    private fun observeSaveState() {
        lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                if (state !is SaveJournalState.Loading) {
                    hideLoading()
                }
                when (state) {
                    is SaveJournalState.Loading -> {
                        showLoading("Đang lưu nhật ký...")
                    }

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
                            Toast.makeText(requireContext(), "Đã lưu thành công!", Toast.LENGTH_SHORT).show()
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

    private fun showConsistencyDialog(analysis: AIAnalysis) {
        val currentMood = viewModel.currentMood.value

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🤔 AI có một chút băn khoăn...")
            .setMessage("Bạn chọn cảm xúc là '$currentMood', nhưng AI cảm thấy nội dung lại thiên về '${analysis.suggestedEmotion}'.\n\n" +
                    "Lời nhắn: \"${analysis.advice}\"\n\n" +
                    "Bạn có muốn đổi sang '${analysis.suggestedEmotion}' cho phù hợp không?")
            .setCancelable(false)
            .setPositiveButton("Đổi thành '${analysis.suggestedEmotion}'") { dialog, _ ->
                dialog.dismiss()
                
                viewModel.quickUpdateEmotion(analysis.suggestedEmotion)
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

    private fun updateEmotionUI(selectedButton: ImageButton) {
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