package com.Plant_application.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.ItemPlantBinding
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.TimeUnit

class PlantAdapter(
    private val onItemClicked: (PlantItem) -> Unit,
    private val onWaterClicked: (PlantItem) -> Unit,
    private val onPesticideClicked: (PlantItem) -> Unit
) : ListAdapter<PlantItem, PlantAdapter.PlantViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding, onWaterClicked, onPesticideClicked)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = getItem(position)
        holder.itemView.setOnClickListener { onItemClicked(plant) }
        holder.bind(plant)
    }

    class PlantViewHolder(
        private val binding: ItemPlantBinding,
        private val onWaterClicked: (PlantItem) -> Unit,
        private val onPesticideClicked: (PlantItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plantItem: PlantItem) {
            binding.tvPlantNickname.text = plantItem.nickname
            binding.tvLifespan.text = formatRange(plantItem.lifespanMin, plantItem.lifespanMax, "년")

            binding.tvReasonWater.isVisible = plantItem.attentionReasons?.contains("WATER") == true
            binding.tvReasonPesticide.isVisible = plantItem.attentionReasons?.contains("PESTICIDE") == true
            binding.tvReasonTemp.isVisible = plantItem.attentionReasons?.contains("TEMP") == true

            val currentTime = System.currentTimeMillis()

            // Water Gauge
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plantItem.wateringCycleMax.toLong())
            if (totalWaterMillis <= 0) {
                binding.tvWaterInfo.text = "물 주기: 필요 없음"
                binding.pbWaterGauge.progress = 100
                binding.pbWaterGauge.secondaryProgress = 100
                binding.btnWaterCheck.visibility = View.GONE
            } else {
                val timeSinceWater = (currentTime - plantItem.lastWateredTimestamp).coerceAtLeast(0L)
                binding.tvWaterInfo.text = "물: ${formatTimeElapsed(timeSinceWater)} 전"
                binding.btnWaterCheck.visibility = View.VISIBLE

                val waterWarningMillis = TimeUnit.DAYS.toMillis(plantItem.wateringCycleMin.toLong())
                val currentWaterPercent = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
                val waterWarningPercent = (100 - (waterWarningMillis * 100 / totalWaterMillis)).coerceIn(0, 100)

                binding.pbWaterGauge.secondaryProgress = currentWaterPercent.toInt()
                binding.pbWaterGauge.progress = waterWarningPercent.toInt()
            }

            // Pesticide Gauge
            val totalPesticideMillis = TimeUnit.DAYS.toMillis(plantItem.pesticideCycleMax.toLong())
            if (totalPesticideMillis <= 0) {
                binding.tvPesticideInfo.text = "살충제: 필요 없음"
                binding.pbPesticideGauge.progress = 100
                binding.pbPesticideGauge.secondaryProgress = 100
                binding.btnPesticideCheck.visibility = View.GONE
            } else {
                val timeSincePesticide = (currentTime - plantItem.lastPesticideTimestamp).coerceAtLeast(0L)
                binding.tvPesticideInfo.text = "살충제: ${formatTimeElapsed(timeSincePesticide)} 전"
                binding.btnPesticideCheck.visibility = View.VISIBLE

                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plantItem.pesticideCycleMin.toLong())
                val currentPesticidePercent = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                val pesticideWarningPercent = (100 - (pesticideWarningMillis * 100 / totalPesticideMillis)).coerceIn(0, 100)

                binding.pbPesticideGauge.secondaryProgress = currentPesticidePercent.toInt()
                binding.pbPesticideGauge.progress = pesticideWarningPercent.toInt()
            }

            Glide.with(itemView.context)
                .load(Uri.fromFile(File(plantItem.imageUri)))
                .into(binding.ivPlantImage)

            binding.btnWaterCheck.setOnClickListener { onWaterClicked(plantItem) }
            binding.btnPesticideCheck.setOnClickListener { onPesticideClicked(plantItem) }
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