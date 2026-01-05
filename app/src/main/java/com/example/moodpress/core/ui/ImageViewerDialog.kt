package com.example.moodpress.core.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.moodpress.databinding.DialogImageViewerBinding

class ImageViewerDialog : DialogFragment() {

    private var _binding: DialogImageViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
        Glide.with(this)
            .load(imageUrl)
            .into(binding.photoView)
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ImageViewerDialog"
        private const val ARG_IMAGE_URL = "arg_image_url"
        fun show(fragmentManager: androidx.fragment.app.FragmentManager, imageSource: Any) {
            val dialog = ImageViewerDialog()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageSource.toString())
            dialog.arguments = args
            dialog.show(fragmentManager, TAG)
        }
    }
}