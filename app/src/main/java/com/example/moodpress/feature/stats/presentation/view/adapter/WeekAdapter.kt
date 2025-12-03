package com.example.moodpress.feature.stats.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.moodpress.databinding.ItemWeekOptionBinding // (Tạo layout xml đơn giản chỉ có 1 TextView)
import com.example.moodpress.feature.stats.domain.model.WeekOption

class WeekAdapter(
    private val weeks: List<WeekOption>,
    private val onWeekSelected: (WeekOption) -> Unit
) : RecyclerView.Adapter<WeekAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeekOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = weeks[position]
        holder.binding.tvWeekLabel.text = item.label
        holder.itemView.setOnClickListener { onWeekSelected(item) }
    }

    override fun getItemCount() = weeks.size

    class ViewHolder(val binding: ItemWeekOptionBinding) : RecyclerView.ViewHolder(binding.root)
}