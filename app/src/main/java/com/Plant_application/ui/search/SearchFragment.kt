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
import androidx.navigation.fragment.findNavController
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
        // 권한 결과와 상관없이 로드 시도 (권한 없으면 설문 기반만이라도 뜨도록)
        viewModel.loadRecommendations()

        if (!isGranted) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // 다시 묻지 않음 상태일 때만 다이얼로그 표시 고민 (여기서는 Toast만)
                Toast.makeText(context, "위치 권한이 없어 날씨 기반 추천은 제외됩니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)


        observeViewModel()
        checkPermissionAndLoad()
    }



    private fun checkPermissionAndLoad() {
        // 이미 데이터가 있거나 로딩 중이면 스킵
        if (viewModel.isLoadingRecommendations.value == true ||
            (viewModel.surveyRecommendation.value != null || viewModel.weatherRecommendation.value != null)) {
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
                        viewModel.loadRecommendations() // 거부해도 설문 기반은 로드
                    }
                    .show()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoadingRecommendations.observe(viewLifecycleOwner) { isLoading ->
            binding.pbRecommendations.isVisible = isLoading
            updateUIState()
        }

        viewModel.recommendationError.observe(viewLifecycleOwner) {
            updateUIState()
        }

        viewModel.surveyRecommendation.observe(viewLifecycleOwner) { analysis ->
            updateSurveyCard(analysis)
            updateUIState()
        }

        viewModel.weatherRecommendation.observe(viewLifecycleOwner) { analysis ->
            updateWeatherCard(analysis)
            updateUIState()
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

    private fun updateUIState() {
        val isLoading = viewModel.isLoadingRecommendations.value ?: false
        val hasSurvey = viewModel.surveyRecommendation.value != null
        val hasWeather = viewModel.weatherRecommendation.value != null
        val errorMsg = viewModel.recommendationError.value

        // 로딩 중이면 에러 숨김
        if (isLoading) {
            binding.tvRecommendationError.isVisible = false
            return
        }

        // 로딩 끝났는데 둘 다 없으면 에러(또는 안내) 표시
        if (!hasSurvey && !hasWeather) {
            binding.tvRecommendationError.isVisible = true
            binding.tvRecommendationError.text = errorMsg ?: "추천할 수 있는 식물이 없습니다."
        } else {
            // 하나라도 있으면 에러 숨김
            binding.tvRecommendationError.isVisible = false
        }
    }

    private fun updateSurveyCard(analysis: PlantAnalysis?) {
        if (analysis != null) {
            binding.cardSurveyRec.isVisible = true
            binding.tvSurveyTitle.text = analysis.official_name
            binding.tvSurveyDetails.text = formatAnalysisDetails(analysis)
        } else {
            binding.cardSurveyRec.isVisible = false
        }
    }

    private fun updateWeatherCard(analysis: PlantAnalysis?) {
        if (analysis != null) {
            binding.cardWeatherRec.isVisible = true
            binding.tvWeatherTitle.text = analysis.official_name
            binding.tvWeatherDetails.text = formatAnalysisDetails(analysis)
        } else {
            binding.cardWeatherRec.isVisible = false
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