package com.Plant_application.ui.search

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.R
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.ItemPlantBinding
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.TimeUnit

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
            binding.tvLifespan.text = "${plant.lifespanMin}-${plant.lifespanMax}년"

            val currentTime = System.currentTimeMillis()
            val context = itemView.context

            // Water Gauge
            val timeSinceWater = currentTime - plant.lastWateredTimestamp
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMax.toLong())
            if (totalWaterMillis > 0) {
                val waterProgress = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
                binding.pbWaterGauge.progress = waterProgress.toInt()
            } else {
                binding.pbWaterGauge.progress = 100
            }

            val waterWarningMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMin.toLong())
            val waterColor = if (timeSinceWater > waterWarningMillis) R.color.warning_orange else R.color.primary
            binding.pbWaterGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, waterColor))

            // Pesticide Gauge
            if (plant.pesticideCycleMax <= 0) {
                binding.pbPesticideGauge.progress = 100
                binding.pbPesticideGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
            } else {
                val timeSincePesticide = currentTime - plant.lastPesticideTimestamp
                val totalPesticideMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMax.toLong())
                val pesticideProgress = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                binding.pbPesticideGauge.progress = pesticideProgress.toInt()

                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMin.toLong())
                val pesticideColor = if (timeSincePesticide > pesticideWarningMillis) R.color.warning_orange else R.color.primary
                binding.pbPesticideGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, pesticideColor))
            }

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