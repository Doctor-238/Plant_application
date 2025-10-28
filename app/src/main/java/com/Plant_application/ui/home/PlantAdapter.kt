package com.Plant_application.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.ItemPlantBinding
import com.bumptech.glide.Glide
import java.io.File

class PlantAdapter(private val onItemClicked: (PlantItem) -> Unit) : ListAdapter<PlantItem, PlantAdapter.PlantViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = getItem(position)
        holder.itemView.setOnClickListener { onItemClicked(plant) }
        holder.bind(plant)
    }

    class PlantViewHolder(private val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plantItem: PlantItem) {
            binding.tvPlantNickname.text = plantItem.nickname
            binding.tvPlantOfficialName.text = plantItem.officialName
            binding.tvWateringInfo.text = "물주기: ${plantItem.wateringCycle}"

            Glide.with(itemView.context)
                .load(Uri.fromFile(File(plantItem.imageUri)))
                .into(binding.ivPlantImage)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PlantItem>() {
            override fun areItemsTheSame(oldItem: PlantItem, newItem: PlantItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PlantItem, newItem: PlantItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
