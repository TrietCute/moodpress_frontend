package com.example.moodpress.core.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.moodpress.databinding.DialogImageViewerBinding

class ImageViewerDialog(private val imageUrl: Any) : DialogFragment() {

    private var _binding: DialogImageViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cài đặt Style để Full Screen và trong suốt
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Load ảnh bằng Glide
        Glide.with(this)
            .load(imageUrl)
            .into(binding.photoView)

        // 2. Nút Đóng
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ImageViewerDialog"
        fun show(fragmentManager: androidx.fragment.app.FragmentManager, imageUrl: Any) {
            val dialog = ImageViewerDialog(imageUrl)
            dialog.show(fragmentManager, TAG)
        }
    }
}