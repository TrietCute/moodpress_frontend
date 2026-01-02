package com.example.moodpress.feature.journal.presentation.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moodpress.databinding.DialogJournalOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class JournalOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogJournalOptionsBinding? = null
    private val binding get() = _binding!!

    private var listener: OptionsListener? = null

    interface OptionsListener {
        fun onEditClicked()
        fun onDeleteClicked()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? OptionsListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogJournalOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        with(binding) {
            optionEdit.setOnClickListener {
                listener?.onEditClicked()
                dismiss()
            }

            optionDelete.setOnClickListener {
                listener?.onDeleteClicked()
                dismiss()
            }

            optionCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "JournalOptionsBottomSheet"
    }
}