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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodpress.R
import com.example.moodpress.core.ui.ImageViewerDialog
import com.example.moodpress.core.ui.hideLoading
import com.example.moodpress.core.ui.showLoading
import com.example.moodpress.databinding.FragmentCreateJournalBinding
import com.example.moodpress.feature.journal.domain.model.AIAnalysis
import com.example.moodpress.feature.journal.domain.model.JournalEntry
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

    private val imageAdapter by lazy {
        ImagePreviewAdapter(
            onImageClick = { imageSource -> ImageViewerDialog.show(childFragmentManager, imageSource) },
            onDeleteClick = { position -> viewModel.removeImageAt(position) }
        )
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    private var selectedDate: Date = Date()
    private lateinit var emotionButtons: List<ImageButton>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        handleArguments()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupViews() {
        // Init Recycler
        binding.recyclerImages.apply {
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Init Emotion Buttons
        emotionButtons = binding.emotionLayout.children.mapNotNull { it as? ImageButton }.toList()

        // Set Default Mood if empty
        if (viewModel.currentMood.value.isBlank()) {
            viewModel.updateJournalEmotion(binding.emotionNeutral.contentDescription.toString())
        }
    }

    private fun handleArguments() {
        val passedDateMillis = args.selectedDate
        val journalId = args.journalId

        if (journalId != null) {
            // Edit mode
            binding.buttonDatePicker.isEnabled = false
            binding.buttonDatePicker.alpha = 0.5f
        } else if (passedDateMillis != -1L) {
            selectedDate = Date(passedDateMillis)
            updateDateButtonText(passedDateMillis)
        } else {
            updateDateButtonText(selectedDate.time)
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            buttonClose.setOnClickListener { findNavController().popBackStack() }

            buttonDatePicker.setOnClickListener { showDatePicker() }

            btnAddImage.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            buttonSave.setOnClickListener { performSave() }
        }

        emotionButtons.forEach { button ->
            button.setOnClickListener {
                viewModel.updateJournalEmotion(button.contentDescription.toString())
            }
        }
    }

    private fun performSave(isSilent: Boolean = false) {
        viewModel.saveJournal(
            context = requireContext().applicationContext,
            content = binding.contentEditText.text.toString(),
            emotion = viewModel.currentMood.value,
            selectedDate = selectedDate,
            isSilent = isSilent
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeSaveState() }
                launch { observeJournalData() }
                launch { observeImages() }
                launch { observeCurrentMood() }
            }
        }
    }

    private suspend fun observeSaveState() {
        viewModel.saveState.collect { state ->
            if (state !is SaveJournalState.Loading) hideLoading()

            when (state) {
                is SaveJournalState.Loading -> showLoading("Đang lưu nhật ký...")
                is SaveJournalState.Success -> handleSaveSuccess(state)
                is SaveJournalState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                else -> {}
            }
        }
    }

    private fun handleSaveSuccess(state: SaveJournalState.Success) {
        viewModel.resetState()

        if (state.isSilent) {
            Toast.makeText(requireContext(), "Đã cập nhật cảm xúc!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val analysis = state.entry.analysis
        when {
            analysis != null && !analysis.isMatch -> showConsistencyDialog(analysis)
            analysis?.advice?.isNotEmpty() == true -> showAdviceDialog(analysis.advice)
            else -> {
                Toast.makeText(requireContext(), "Đã lưu thành công!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private suspend fun observeJournalData() {
        viewModel.journalData.collect { entry ->
            entry?.let {
                binding.contentEditText.setText(it.content)
                updateDateButtonText(it.timestamp.time)
                selectedDate = it.timestamp
            }
        }
    }

    private suspend fun observeImages() {
        viewModel.currentImages.collect { images ->
            imageAdapter.submitList(images)
            binding.recyclerImages.isVisible = images.isNotEmpty()
            binding.btnAddImage.isVisible = images.size < 5
        }
    }

    private suspend fun observeCurrentMood() {
        viewModel.currentMood.collect { mood ->
            emotionButtons.forEach { button ->
                button.alpha = if (button.contentDescription.toString() == mood) 1.0f else 0.5f
            }
        }
    }

    // --- Dialogs & Pickers ---

    private fun showConsistencyDialog(analysis: AIAnalysis) {
        val currentMood = viewModel.currentMood.value
        val message = "Bạn chọn cảm xúc là '$currentMood', nhưng AI cảm thấy nội dung lại thiên về '${analysis.suggestedEmotion}'.\n\n" +
                "Lời nhắn: \"${analysis.advice}\"\n\n" +
                "Bạn có muốn đổi sang '${analysis.suggestedEmotion}' cho phù hợp không?"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🤔 AI có một chút băn khoăn...")
            .setMessage(message)
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
            .setCancelable(false)
            .setPositiveButton("Cảm ơn") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .show()
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()
            .apply {
                addOnPositiveButtonClickListener { utcMillis ->
                    selectedDate = Date(utcMillis)
                    updateDateButtonText(utcMillis)
                }
            }
            .show(childFragmentManager, "DATE_PICKER")
    }

    private fun updateDateButtonText(millis: Long) {
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("vi", "VN"))
        binding.buttonDatePicker.text = sdf.format(Date(millis))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}