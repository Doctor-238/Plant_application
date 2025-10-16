package com.Plant_application.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.Plant_application.R
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.databinding.FragmentOnboardingBinding
import com.google.android.material.chip.Chip

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        binding.btnRecommend.setOnClickListener {
            val location = binding.etLocation.text.toString()
            val wateringChip = binding.chipGroupWatering.findViewById<Chip>(binding.chipGroupWatering.checkedChipId)
            val sunlightChip = binding.chipGroupSunlight.findViewById<Chip>(binding.chipGroupSunlight.checkedChipId)

            if (location.isBlank() || wateringChip == null || sunlightChip == null) {
                Toast.makeText(requireContext(), "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.getPlantRecommendation(
                getString(R.string.gemini_api_key),
                location,
                wateringChip.text.toString(),
                sunlightChip.text.toString()
            )
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnRecommend.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.recommendationResult.observe(viewLifecycleOwner) { result ->
            result?.let { analysisResult ->
                // 온보딩 완료 처리
                val prefs = PreferenceManager(requireContext())
                prefs.isFirstLaunch = false

                // AI 추천 결과를 AddPlantFragment로 전달하며 화면 전환
                val action = OnboardingFragmentDirections.actionOnboardingFragmentToAddPlantFragment(analysisResult)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}