package com.Plant_application.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.R
import com.Plant_application.data.database.DiaryEntry
import com.Plant_application.databinding.ItemDiaryEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryAdapter(
    private val onItemClick: (DiaryEntry) -> Unit,
    private val onItemLongClick: (DiaryEntry) -> Unit,
    private val isDeleteMode: () -> Boolean,
    private val isSelected: (Long) -> Boolean
) : ListAdapter<DiaryEntry, DiaryAdapter.DiaryViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemDiaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat, onItemClick, onItemLongClick, isDeleteMode(), isSelected(getItem(position).id))
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload in payloads) {
                if (payload == "DELETE_MODE_CHANGED") holder.updateDeleteModeUI(isDeleteMode())
                if (payload == "SELECTION_CHANGED") holder.updateSelectionUI(isSelected(getItem(position).id))
            }
        }
    }

    class DiaryViewHolder(private val binding: ItemDiaryEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            entry: DiaryEntry,
            dateFormat: SimpleDateFormat,
            onClick: (DiaryEntry) -> Unit,
            onLongClick: (DiaryEntry) -> Unit,
            isDeleteMode: Boolean,
            isSelected: Boolean
        ) {
            binding.tvDiaryDate.text = dateFormat.format(Date(entry.timestamp))
            binding.tvDiaryContent.text = entry.content

            itemView.setOnClickListener { onClick(entry) }
            itemView.setOnLongClickListener {
                onLongClick(entry)
                true
            }

            updateDeleteModeUI(isDeleteMode)
            updateSelectionUI(isSelected)
        }

        fun updateDeleteModeUI(isDeleteMode: Boolean) {
            if (isDeleteMode) {
                if (binding.ivDeleteCheckbox.visibility != View.VISIBLE) {
                    binding.ivDeleteCheckbox.visibility = View.VISIBLE
                    binding.ivDeleteCheckbox.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_in))
                }
            } else {
                if (binding.ivDeleteCheckbox.visibility == View.VISIBLE) {
                    binding.ivDeleteCheckbox.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_out))
                    binding.ivDeleteCheckbox.visibility = View.INVISIBLE
                } else if (binding.ivDeleteCheckbox.visibility == View.GONE) {
                    binding.ivDeleteCheckbox.visibility = View.INVISIBLE
                }
            }
        }

        fun updateSelectionUI(isSelected: Boolean) {
            binding.ivDeleteCheckbox.setImageResource(
                if (isSelected) R.drawable.ic_checkbox_checked_custom else R.drawable.ic_checkbox_unchecked_custom
            )
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