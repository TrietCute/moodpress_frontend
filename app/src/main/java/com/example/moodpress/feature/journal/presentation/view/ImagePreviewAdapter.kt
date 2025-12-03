package com.example.moodpress.feature.journal.presentation.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moodpress.databinding.ItemImagePreviewBinding // (Bạn cần tạo layout này)

class ImagePreviewAdapter(
    private val onImageClick: (Any) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    private val items = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any, position: Int) {
            Glide.with(binding.root)
                .load(item)
                .centerCrop()
                .into(binding.imgThumb)

            binding.btnDelete.setOnClickListener {
                onDeleteClick(position)
            }

            binding.imgThumb.setOnClickListener {
                onImageClick(item)
            }
        }
    }
}