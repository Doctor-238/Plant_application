package com.Plant_application.ui.journal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.data.database.DiaryEntry
import com.Plant_application.databinding.ItemDiaryEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryAdapter : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat)
    }

    class DiaryViewHolder(private val binding: ItemDiaryEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: DiaryEntry, dateFormat: SimpleDateFormat) {
            binding.tvDiaryDate.text = dateFormat.format(Date(entry.timestamp))
            binding.tvDiaryContent.text = entry.content
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DiaryEntry>() {
            override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}