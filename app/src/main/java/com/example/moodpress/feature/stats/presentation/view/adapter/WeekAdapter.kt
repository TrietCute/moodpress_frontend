package com.example.moodpress.feature.stats.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.databinding.ItemWeekOptionBinding
import com.example.moodpress.feature.stats.domain.model.WeekOption

class WeekAdapter(
    private val weeks: List<WeekOption>,
    private val onWeekSelected: (WeekOption) -> Unit
) : RecyclerView.Adapter<WeekAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeekOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(weeks[position])
    }

    override fun getItemCount() = weeks.size

    inner class ViewHolder(private val binding: ItemWeekOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WeekOption) {
            binding.tvWeekLabel.text = item.label

            binding.root.setOnClickListener {
                onWeekSelected(item)
            }
        }
    }
}