package com.Plant_application.ui.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.Plant_application.R
import com.Plant_application.databinding.FragmentSearchBinding
import com.Plant_application.ui.add.PlantAnalysis
import com.bumptech.glide.Glide

class SearchFragment : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by navGraphViewModels(R.id.mobile_navigation)

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadRecommendations()
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showGoToSettingsDialog()
            } else {
                Toast.makeText(context, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
            viewModel.loadRecommendations()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        observeViewModel()
        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        if (viewModel.isLoadingRecommendations.value == true || (viewModel.surveyRecommendation.value != null || viewModel.weatherRecommendation.value != null)) {
            return
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadRecommendations()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("위치 권한 안내")
                    .setMessage("현재 날씨 기반 식물 추천을 위해 위치 권한이 필요합니다.")
                    .setPositiveButton("권한 허용") { _, _ ->
                        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    .setNegativeButton("거부") { _, _ ->
                        viewModel.loadRecommendations()
                    }
                    .show()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("권한 필요")
            .setMessage("날씨 정보를 위해 위치 권한이 반드시 필요합니다. '설정'으로 이동하여 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("닫기", null)
            .setCancelable(false)
            .show()
    }

    private fun observeViewModel() {
        viewModel.isLoadingRecommendations.observe(viewLifecycleOwner) { isLoading ->
            binding.pbRecommendations.isVisible = isLoading
            if (isLoading) {
                binding.tvRecommendationError.isVisible = false
            }
        }

        viewModel.recommendationError.observe(viewLifecycleOwner) { error ->
            val isLoading = viewModel.isLoadingRecommendations.value ?: false
            binding.tvRecommendationError.isVisible = !isLoading && error != null
            binding.tvRecommendationError.text = error
        }

        viewModel.surveyRecommendation.observe(viewLifecycleOwner) { analysis ->
            if (analysis != null) {
                binding.cardSurveyRec.isVisible = true
                binding.tvSurveyTitle.text = analysis.official_name
                binding.tvSurveyDetails.text = formatAnalysisDetails(analysis)
            } else {
                binding.cardSurveyRec.isVisible = false
            }
        }

        viewModel.weatherRecommendation.observe(viewLifecycleOwner) { analysis ->
            if (analysis != null) {
                binding.cardWeatherRec.isVisible = true
                binding.tvWeatherTitle.text = analysis.official_name
                binding.tvWeatherDetails.text = formatAnalysisDetails(analysis)
            } else {
                binding.cardWeatherRec.isVisible = false
            }
        }

        viewModel.surveyRecommendationImage.observe(viewLifecycleOwner) { imageUrl ->
            imageUrl?.let {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.plant2)
                    .into(binding.ivSurveyImage)
            }
        }

        viewModel.weatherRecommendationImage.observe(viewLifecycleOwner) { imageUrl ->
            imageUrl?.let {
                Glide.with(this)
                    .load(it)
                    .placeholder(R.drawable.plant1)
                    .into(binding.ivWeatherImage)
            }
        }
    }

    private fun formatAnalysisDetails(analysis: PlantAnalysis): String {
        val waterMin = analysis.watering_cycle_min_days ?: 0
        val waterMax = analysis.watering_cycle_max_days ?: 0
        val water = "물 주기: ${if (waterMin == waterMax) "$waterMax" else "$waterMin-$waterMax"}일"

        val temp = "적정 온도: ${analysis.temp_range ?: "N/A"}"

        val pestMin = analysis.pesticide_cycle_min_days ?: 0
        val pestMax = analysis.pesticide_cycle_max_days ?: 0
        val pesticide = "살충제: ${if (pestMax <= 0) "필요 없음" else if (pestMin == pestMax) "$pestMax" else "$pestMin-$pestMax"}일"

        val lifeMin = analysis.lifespan_min_years ?: 0
        val lifeMax = analysis.lifespan_max_years ?: 0
        val lifespan = "수명: ${if (lifeMax <= 0) "N/A" else if (lifeMin == lifeMax) "$lifeMax" else "$lifeMin-$lifeMax"}년"

        return "$water\n$temp\n$pesticide\n$lifespan"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}