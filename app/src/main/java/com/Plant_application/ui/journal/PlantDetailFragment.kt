package com.Plant_application.ui.journal

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.Plant_application.R
import com.Plant_application.data.database.PlantItem
import com.Plant_application.databinding.FragmentPlantDetailBinding
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.TimeUnit

class PlantDetailFragment : Fragment(R.layout.fragment_plant_detail) {

    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

    private var currentPlant: PlantItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlantDetailBinding.bind(view)

        setupToolbar()

        viewModel.loadPlant(args.plantId)
        observeViewModel()

        binding.fabEdit.setOnClickListener {
            val action = PlantDetailFragmentDirections.actionPlantDetailFragmentToEditPlantFragment(args.plantId)
            findNavController().navigate(action)
        }

        binding.fabDiary.setOnClickListener {
            currentPlant?.let { plant ->
                val action = PlantDetailFragmentDirections.actionPlantDetailFragmentToDiaryListFragment(
                    plant.id,
                    plant.nickname,
                    plant.imageUri
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun observeViewModel() {
        viewModel.plantItem.observe(viewLifecycleOwner) { plant ->
            if (plant == null) return@observe

            currentPlant = plant

            binding.toolbarLayout.title = plant.nickname
            Glide.with(this).load(Uri.fromFile(File(plant.imageUri))).into(binding.ivPlantImage)

            binding.tvOfficialName.text = "공식 이름: ${plant.officialName}"
            binding.tvHealth.text = "건강 상태: ${"★".repeat(plant.healthRating.toInt())}${"☆".repeat(5 - plant.healthRating.toInt())}"
            binding.tvTemp.text = "적정 온도: ${plant.tempRange}"

            val currentTime = System.currentTimeMillis()

            binding.pbWater.visibility = View.VISIBLE
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMax.toLong())
            if (totalWaterMillis <= 0) {
                binding.tvWatering.text = "물 주기: 필요 없음"
                binding.pbWater.progress = 100
                binding.pbWater.secondaryProgress = 100
            } else {
                val timeSinceWater = (currentTime - plant.lastWateredTimestamp).coerceAtLeast(0L)
                val waterRange = formatRange(plant.wateringCycleMin, plant.wateringCycleMax, "일")
                binding.tvWatering.text = "물 주기: $waterRange (마지막: ${formatTimeElapsed(timeSinceWater)} 전)"

                val waterWarningMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMin.toLong())
                val currentWaterPercent = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
                val waterWarningPercent = (100 - (waterWarningMillis * 100 / totalWaterMillis)).coerceIn(0, 100)

                binding.pbWater.secondaryProgress = currentWaterPercent.toInt()
                binding.pbWater.progress = waterWarningPercent.toInt()
            }

            binding.pbPesticide.visibility = View.VISIBLE
            val totalPesticideMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMax.toLong())
            if (totalPesticideMillis <= 0) {
                binding.tvPesticide.text = "살충제: 필요 없음"
                binding.pbPesticide.progress = 100
                binding.pbPesticide.secondaryProgress = 100
            } else {
                val timeSincePesticide = (currentTime - plant.lastPesticideTimestamp).coerceAtLeast(0L)
                val pesticideRange = formatRange(plant.pesticideCycleMin, plant.pesticideCycleMax, "일")
                binding.tvPesticide.text = "살충제: $pesticideRange (마지막: ${formatTimeElapsed(timeSincePesticide)} 전)"

                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMin.toLong())
                val currentPesticidePercent = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                val pesticideWarningPercent = (100 - (pesticideWarningMillis * 100 / totalPesticideMillis)).coerceIn(0, 100)

                binding.pbPesticide.secondaryProgress = currentPesticidePercent.toInt()
                binding.pbPesticide.progress = pesticideWarningPercent.toInt()
            }

            binding.tvLifespan.text = "예상 수명: ${formatRange(plant.lifespanMin, plant.lifespanMax, "년")}"
            val totalLifespanMillis = TimeUnit.DAYS.toMillis(plant.lifespanMax * 365L)
            if (totalLifespanMillis > 0) {
                binding.pbLifespan.visibility = View.VISIBLE
                val ageInMillis = TimeUnit.DAYS.toMillis(plant.estimatedAge.toLong()) + (currentTime - plant.timestamp)
                val lifespanWarningMillis = TimeUnit.DAYS.toMillis(plant.lifespanMin * 365L)

                val currentLifespanPercent = (100 - (ageInMillis * 100 / totalLifespanMillis)).coerceIn(0, 100)
                val lifespanWarningPercent = (100 - (lifespanWarningMillis * 100 / totalLifespanMillis)).coerceIn(0, 100)

                binding.pbLifespan.secondaryProgress = currentLifespanPercent.toInt()
                binding.pbLifespan.progress = lifespanWarningPercent.toInt()
            } else {
                binding.pbLifespan.visibility = View.GONE
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}