package com.Plant_application.ui.journal

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.R
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.ItemPlantJournalBinding
import com.bumptech.glide.Glide
import java.io.File

class JournalAdapter(
    private val onItemClicked: (PlantItem) -> Unit,
    private val onItemLongClicked: (PlantItem) -> Unit,
    private val isDeleteMode: () -> Boolean,
    private val isItemSelected: (Int) -> Boolean
) : ListAdapter<PlantItem, JournalAdapter.PlantViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked, onItemLongClicked, isDeleteMode, isItemSelected)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload in payloads) {
                if (payload == "DELETE_MODE_CHANGED") holder.updateDeleteModeUI(isDeleteMode())
                if (payload == "SELECTION_CHANGED") holder.updateSelectionUI(isItemSelected(getItem(position).id))
            }
        }
    }

    class PlantViewHolder(private val binding: ItemPlantJournalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            plant: PlantItem,
            clickAction: (PlantItem) -> Unit,
            longClickAction: (PlantItem) -> Unit,
            isDeleteMode: () -> Boolean,
            isItemSelected: (Int) -> Boolean
        ) {
            binding.tvPlantNickname.text = plant.nickname
            binding.tvWateringInfo.text = "물 주기: ${plant.wateringCycle}"
            binding.ratingBarHealth.rating = plant.healthRating

            Glide.with(itemView.context).load(Uri.fromFile(File(plant.imageUri))).into(binding.ivPlantImage)

            itemView.setOnClickListener { clickAction(plant) }
            itemView.setOnLongClickListener {
                if (!isDeleteMode()) longClickAction(plant)
                true
            }

            updateDeleteModeUI(isDeleteMode())
            updateSelectionUI(isItemSelected(plant.id))
        }

        fun updateDeleteModeUI(isDelete: Boolean) {
            if (isDelete && binding.ivDeleteCheckbox.visibility == View.GONE) {
                binding.ivDeleteCheckbox.visibility = View.VISIBLE
                binding.ivDeleteCheckbox.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_in))
            } else if (!isDelete && binding.ivDeleteCheckbox.visibility == View.VISIBLE) {
                binding.ivDeleteCheckbox.startAnimation(AnimationUtils.loadAnimation(itemView.context, R.anim.scale_out))
                binding.ivDeleteCheckbox.visibility = View.GONE
            }
        }

        fun updateSelectionUI(isSelected: Boolean) {
            binding.ivDeleteCheckbox.setImageResource(
                if (isSelected) R.drawable.ic_checkbox_checked_custom else R.drawable.ic_checkbox_unchecked_custom
            )
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PlantItem>() {
            override fun areItemsTheSame(oldItem: PlantItem, newItem: PlantItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PlantItem, newItem: PlantItem): Boolean = oldItem == newItem
        }
    }
}