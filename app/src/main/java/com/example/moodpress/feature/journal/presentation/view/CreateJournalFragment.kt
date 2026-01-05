package com.example.moodpress.feature.journal.presentation.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    private val moodAdapter by lazy {
        MoodAdapter { selectedMood ->
            viewModel.updateJournalEmotion(selectedMood.value)
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupData()
        setupViews()
        handleArguments()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupData() {
        val moodList = listOf(
            MoodItem("Rất tệ", "Rất tệ", R.drawable.ic_emotion_very_dissatisfied, R.color.emotion_very_dissatisfied),
            MoodItem("Tệ", "Tệ", R.drawable.ic_emotion_dissatisfied, R.color.emotion_dissatisfied),
            MoodItem("Bình thường", "Bình thường", R.drawable.ic_emotion_neutral, R.color.emotion_neutral),
            MoodItem("Tốt", "Tốt", R.drawable.ic_emotion_satisfied, R.color.emotion_satisfied),
            MoodItem("Rất tốt", "Rất tốt", R.drawable.ic_emotion_very_satisfied, R.color.emotion_very_satisfied)
        )
        moodAdapter.submitList(moodList)
        if (viewModel.currentMood.value.isBlank()) {
            viewModel.updateJournalEmotion("Bình thường")
        }
    }

    private fun setupViews() {
        with(binding) {
            recyclerImages.apply {
                adapter = imageAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }

            recyclerEmotions.apply {
                adapter = moodAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                itemAnimator = null
            }

            contentEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val length = s?.length ?: 0
                    tvCharCount.text = "$length ký tự"
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun handleArguments() {
        val passedDateMillis = args.selectedDate
        val journalId = args.journalId

        if (journalId != null) {
            binding.buttonDatePicker.isEnabled = false
            binding.buttonDatePicker.alpha = 0.6f
            binding.toolbar.title = "Chỉnh sửa nhật ký"
        } else {
            binding.toolbar.title = "Nhật ký mới"
            if (passedDateMillis != -1L) {
                viewModel.setEntryDate(passedDateMillis)
            }
        }
    }

    private fun setupClickListeners() {
        with(binding) {
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            buttonSave.setOnClickListener {
                performSave()
            }

            buttonDatePicker.setOnClickListener {
                showDatePicker()
            }

            btnAddImage.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    private fun performSave(isSilent: Boolean = false) {
        viewModel.saveJournal(
            context = requireContext().applicationContext,
            content = binding.contentEditText.text.toString(),
            emotion = viewModel.currentMood.value,
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
                launch {
                    viewModel.entryDate.collect { date ->
                        updateDateDisplay(date.time)
                    }
                }
            }
        }
    }

    private suspend fun observeCurrentMood() {
        viewModel.currentMood.collect { mood ->
            moodAdapter.setSelectedMood(mood)
        }
    }

    private suspend fun observeImages() {
        viewModel.currentImages.collect { images ->
            imageAdapter.submitList(images)
            binding.recyclerImages.isVisible = images.isNotEmpty()
            binding.btnAddImage.alpha = if (images.size >= 5) 0.5f else 1.0f
        }
    }

    private suspend fun observeJournalData() {
        viewModel.journalData.collect { entry ->
            entry?.let {
                if (binding.contentEditText.text.isNullOrEmpty()) {
                    binding.contentEditText.setText(it.content)
                }
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

    // --- Dialogs & UI Utils ---

    private fun showDatePicker() {
        val currentSelection = viewModel.entryDate.value.time

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.before(System.currentTimeMillis() + 86400000))
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày")
            .setSelection(currentSelection)
            .setCalendarConstraints(constraints)
            .build()
            .apply {
                addOnPositiveButtonClickListener { utcMillis ->
                    viewModel.setEntryDate(utcMillis)
                }
            }
            .show(childFragmentManager, "DATE_PICKER")
    }

    private fun updateDateDisplay(millis: Long) {
        val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("vi", "VN"))
        binding.tvDateDisplay.text = sdf.format(Date(millis))
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}