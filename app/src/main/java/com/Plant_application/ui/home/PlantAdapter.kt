package com.Plant_application.ui.home

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
            binding.tvLifespan.text = "${plantItem.lifespanMin}-${plantItem.lifespanMax}년"

            val currentTime = System.currentTimeMillis()
            val context = itemView.context

            // Water Gauge
            val timeSinceWater = currentTime - plantItem.lastWateredTimestamp
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plantItem.wateringCycleMax.toLong())
            if (totalWaterMillis > 0) {
                val waterProgress = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
                binding.pbWaterGauge.progress = waterProgress.toInt()
            } else {
                binding.pbWaterGauge.progress = 100
            }

            val waterWarningMillis = TimeUnit.DAYS.toMillis(plantItem.wateringCycleMin.toLong())
            val waterColor = if (timeSinceWater > waterWarningMillis) R.color.warning_orange else R.color.primary
            binding.pbWaterGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, waterColor))

            // Pesticide Gauge
            if (plantItem.pesticideCycleMax <= 0) {
                binding.pbPesticideGauge.progress = 100
                binding.pbPesticideGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
            } else {
                val timeSincePesticide = currentTime - plantItem.lastPesticideTimestamp
                val totalPesticideMillis = TimeUnit.DAYS.toMillis(plantItem.pesticideCycleMax.toLong())
                val pesticideProgress = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                binding.pbPesticideGauge.progress = pesticideProgress.toInt()

                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plantItem.pesticideCycleMin.toLong())
                val pesticideColor = if (timeSincePesticide > pesticideWarningMillis) R.color.warning_orange else R.color.primary
                binding.pbPesticideGauge.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, pesticideColor))
            }

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