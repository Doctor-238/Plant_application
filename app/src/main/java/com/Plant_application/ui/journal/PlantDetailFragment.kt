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
import com.Plant_application.databinding.FragmentPlantDetailBinding
import com.bumptech.glide.Glide
import java.io.File

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
            binding.tvWatering.text = "물 주기: ${plant.wateringCycle}"
            binding.tvPesticide.text = "살충제: ${plant.pesticideCycle}"
            binding.tvTemp.text = "적정 온도: ${plant.tempRange}"
            binding.tvLifespan.text = "예상 수명: ${plant.lifespan}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
