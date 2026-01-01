package com.example.moodpress.feature.relax.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moodpress.databinding.ItemRelaxSoundBinding
import com.example.moodpress.feature.relax.domain.model.RelaxSound

class RelaxSoundAdapter(
    private val onClick: (RelaxSound) -> Unit
) : RecyclerView.Adapter<RelaxSoundAdapter.SoundViewHolder>() {

    private var sounds = listOf<RelaxSound>()

    fun submitList(newList: List<RelaxSound>) {
        sounds = newList
        notifyDataSetChanged()
    }

    fun updateItem(soundId: String, isPlaying: Boolean) {
        val index = sounds.indexOfFirst { it.id == soundId }
        if (index != -1) {
            sounds[index].isPlaying = isPlaying
            notifyItemChanged(index)
        }
    }

    inner class SoundViewHolder(private val binding: ItemRelaxSoundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sound: RelaxSound) {
            binding.txtName.text = sound.name

            Glide.with(binding.root)
                .load(sound.iconUrl)
                .into(binding.imgIcon)

            binding.viewActiveOverlay.isVisible = sound.isPlaying
            binding.imgPlayStatus.isVisible = sound.isPlaying

            binding.root.setOnClickListener {
                onClick(sound)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemRelaxSoundBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SoundViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(sounds[position])
    }

    override fun getItemCount() = sounds.size
}