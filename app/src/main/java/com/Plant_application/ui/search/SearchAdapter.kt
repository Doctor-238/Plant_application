package com.Plant_application.ui.search

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

class SearchAdapter : ListAdapter<PlantItem, SearchAdapter.SearchViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SearchViewHolder(private val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: PlantItem) {
            binding.tvPlantNickname.text = plant.nickname
            binding.tvPlantOfficialName.text = plant.officialName
            binding.tvWateringInfo.text = "물주기: ${plant.wateringCycle}"

            Glide.with(itemView.context)
                .load(Uri.fromFile(File(plant.imageUri)))
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