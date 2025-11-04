package com.Plant_application.ui.journal

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.Plant_application.R
import com.Plant_application.databinding.FragmentPlantDetailBinding
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.TimeUnit

class PlantDetailFragment : Fragment(R.layout.fragment_plant_detail) {

    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlantDetailViewModel by viewModels()
    private val args: PlantDetailFragmentArgs by navArgs()

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
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun observeViewModel() {
        viewModel.plantItem.observe(viewLifecycleOwner) { plant ->
            if (plant == null) return@observe

            binding.toolbarLayout.title = plant.nickname
            Glide.with(this).load(Uri.fromFile(File(plant.imageUri))).into(binding.ivPlantImage)

            binding.tvOfficialName.text = "공식 이름: ${plant.officialName}"
            binding.tvHealth.text = "건강 상태: ${"★".repeat(plant.healthRating.toInt())}${"☆".repeat(5 - plant.healthRating.toInt())}"
            binding.tvTemp.text = "적정 온도: ${plant.tempRange}"

            val currentTime = System.currentTimeMillis()

            // Water Gauge
            binding.tvWatering.text = "물 주기: ${plant.wateringCycleMin}-${plant.wateringCycleMax}일"
            val timeSinceWater = currentTime - plant.lastWateredTimestamp
            val totalWaterMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMax.toLong())
            val waterProgress = (100 - (timeSinceWater * 100 / totalWaterMillis)).coerceIn(0, 100)
            binding.pbWater.progress = waterProgress.toInt()

            val waterWarningMillis = TimeUnit.DAYS.toMillis(plant.wateringCycleMin.toLong())
            val waterColor = if (timeSinceWater > waterWarningMillis) R.color.warning_orange else R.color.primary
            binding.pbWater.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), waterColor))

            // Pesticide Gauge
            if (plant.pesticideCycleMax <= 0) {
                binding.tvPesticide.text = "살충제: 필요 없음"
                binding.pbPesticide.progress = 100
                binding.pbPesticide.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))
            } else {
                binding.tvPesticide.text = "살충제: ${plant.pesticideCycleMin}-${plant.pesticideCycleMax}일"
                val timeSincePesticide = currentTime - plant.lastPesticideTimestamp
                val totalPesticideMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMax.toLong())
                val pesticideProgress = (100 - (timeSincePesticide * 100 / totalPesticideMillis)).coerceIn(0, 100)
                binding.pbPesticide.progress = pesticideProgress.toInt()

                val pesticideWarningMillis = TimeUnit.DAYS.toMillis(plant.pesticideCycleMin.toLong())
                val pesticideColor = if (timeSincePesticide > pesticideWarningMillis) R.color.warning_orange else R.color.primary
                binding.pbPesticide.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), pesticideColor))
            }

            // Lifespan Gauge
            binding.tvLifespan.text = "예상 수명: ${plant.lifespanMin}-${plant.lifespanMax}년"
            val ageInMillis = TimeUnit.DAYS.toMillis(plant.estimatedAge.toLong()) + (currentTime - plant.timestamp)
            val totalLifespanMillis = TimeUnit.DAYS.toMillis(plant.lifespanMax * 365L)
            val lifespanProgress = (100 - (ageInMillis * 100 / totalLifespanMillis)).coerceIn(0, 100)
            binding.pbLifespan.progress = lifespanProgress.toInt()

            val lifespanWarningMillis = TimeUnit.DAYS.toMillis(plant.lifespanMin * 365L)
            val lifespanColor = if (ageInMillis > lifespanWarningMillis) R.color.warning_orange else R.color.primary
            binding.pbLifespan.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), lifespanColor))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}