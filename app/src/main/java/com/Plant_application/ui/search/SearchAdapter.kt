package com.Plant_application.ui.search

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.tvLifespan.text = formatRange(plant.lifespanMin, plant.lifespanMax, "년")

            val currentTime = System.currentTimeMillis()

            // Water Gauge
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMax.toLong())
            if (totalWaterMillis <= 0) {
                binding.tvWaterInfo.text = "물 주기: 필요 없음"
                binding.pbWaterGauge.progress = 100
                binding.pbWaterGauge.secondaryProgress = 100
            } else {
                val timeSinceWater = (currentTime - plant.lastWateredTimestamp).coerceAtLeast(0L)
                binding.tvWaterInfo.text = "물: ${formatTimeElapsed(timeSinceWater)} 전"
                val waterWarningMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMin.toLong())
                val currentWaterPercent = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
                val waterWarningPercent = (100 - (waterWarningMillis * 100 / totalWaterMillis)).coerceIn(0, 100)

                binding.pbWaterGauge.secondaryProgress = currentWaterPercent.toInt()
                binding.pbWaterGauge.progress = waterWarningPercent.toInt()
            }

            // Pesticide Gauge
            val totalPesticideMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMax.toLong())
            if (totalPesticideMillis <= 0) {
                binding.tvPesticideInfo.text = "살충제: 필요 없음"
                binding.pbPesticideGauge.progress = 100
                binding.pbPesticideGauge.secondaryProgress = 100
            } else {
                val timeSincePesticide = (currentTime - plant.lastPesticideTimestamp).coerceAtLeast(0L)
                binding.tvPesticideInfo.text = "살충제: ${formatTimeElapsed(timeSincePesticide)} 전"
                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMin.toLong())
                val currentPesticidePercent = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                val pesticideWarningPercent = (100 - (pesticideWarningMillis * 100 / totalPesticideMillis)).coerceIn(0, 100)

                binding.pbPesticideGauge.secondaryProgress = currentPesticidePercent.toInt()
                binding.pbPesticideGauge.progress = pesticideWarningPercent.toInt()
            }

            // Hide buttons in search results
            binding.btnWaterCheck.visibility = View.GONE
            binding.btnPesticideCheck.visibility = View.GONE

            Glide.with(itemView.context)
                .load(Uri.fromFile(File(plant.imageUri)))
                .into(binding.ivPlantImage)
        }

        private fun formatRange(min: Int, max: Int, unit: String): String {
            return when {
                max <= 0 -> "알 수 없음"
                min == max -> "$max$unit"
                else -> "$min-$max$unit"
            }
        }

        private fun formatTimeElapsed(millis: Long): String {
            val days = TimeUnit.MILLISECONDS.toDays(millis)
            val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24

            return when {
                days > 0 && hours > 0 -> "${days}일 ${hours}시간"
                days > 0 -> "${days}일"
                hours > 0 -> "${hours}시간"
                else -> "방금"
            }
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