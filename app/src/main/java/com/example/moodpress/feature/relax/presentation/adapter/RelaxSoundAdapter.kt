package com.example.moodpress.feature.relax.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.moodpress.R
import com.example.moodpress.databinding.ItemRelaxSoundBinding
import com.example.moodpress.feature.relax.domain.model.RelaxSound

class RelaxSoundAdapter(
    private val onClick: (RelaxSound) -> Unit
) : ListAdapter<RelaxSound, RelaxSoundAdapter.SoundViewHolder>(SoundDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemRelaxSoundBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SoundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if (payloads.contains(PAYLOAD_PLAY_STATUS)) {
                holder.bindPlayStatus(getItem(position))
            }
        }
    }

    inner class SoundViewHolder(private val binding: ItemRelaxSoundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sound: RelaxSound) {
            binding.txtName.text = sound.name
            Glide.with(binding.root)
                .load(sound.iconUrl)
                .placeholder(R.drawable.bg_rounded_gray_input)
                .error(R.drawable.ic_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.imgIcon)

            bindPlayStatus(sound)
            binding.cardIcon.setOnClickListener {
                onClick(sound)
            }
        }

        fun bindPlayStatus(sound: RelaxSound) {
            val isActive = sound.isPlaying
            binding.viewActiveOverlay.isVisible = isActive
            binding.imgPlayStatus.isVisible = isActive
        }
    }

    class SoundDiffCallback : DiffUtil.ItemCallback<RelaxSound>() {
        override fun areItemsTheSame(oldItem: RelaxSound, newItem: RelaxSound): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RelaxSound, newItem: RelaxSound): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: RelaxSound, newItem: RelaxSound): Any? {
            if (oldItem.isPlaying != newItem.isPlaying) {
                return PAYLOAD_PLAY_STATUS
            }
            return super.getChangePayload(oldItem, newItem)
        }
    }

    companion object {
        private const val PAYLOAD_PLAY_STATUS = "PAYLOAD_PLAY_STATUS"
    }
}