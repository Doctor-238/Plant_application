package com.Plant_application.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.Plant_application.R
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by viewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showGoToSettingsDialog()
            } else {
                showToast("위치 권한이 거부되었습니다. 홈 화면에서 날씨 정보를 이용할 수 없습니다.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        checkLocationPermission()

        binding.btnRecommend.setOnClickListener {
            val spaceRating = getSelectedRating(binding.rgSpace)
            val sunlightRating = getSelectedRating(binding.rgSunlight)
            val tempRating = getSelectedRating(binding.rgTemp)
            val humidityRating = getSelectedRating(binding.rgHumidity)

            val prefs = PreferenceManager(requireContext())
            prefs.surveySpace = spaceRating
            prefs.surveySunlight = sunlightRating
            prefs.surveyTemp = tempRating
            prefs.surveyHumidity = humidityRating

            viewModel.getPlantRecommendation(
                getString(R.string.gemini_api_key),
                spaceRating,
                sunlightRating,
                tempRating,
                humidityRating
            )
        }

        observeViewModel()
    }

    private fun getSelectedRating(radioGroup: RadioGroup): Int {
        val checkedButtonId = radioGroup.checkedRadioButtonId
        val checkedButton = radioGroup.findViewById<RadioButton>(checkedButtonId)
        return radioGroup.indexOfChild(checkedButton) + 1
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnRecommend.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.recommendationResult.observe(viewLifecycleOwner) { result ->
            result?.let { analysisResult ->
                val prefs = PreferenceManager(requireContext())
                prefs.isFirstLaunch = false

                val action = OnboardingFragmentDirections.actionOnboardingFragmentToAddPlantFragment(analysisResult)
                findNavController().navigate(action)
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("위치 권한 안내")
                    .setMessage("앱의 핵심 기능인 날씨 정보 표시를 위해 위치 권한이 필요합니다.")
                    .setPositiveButton("권한 허용") { _, _ ->
                        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    .setNegativeButton("거부", null)
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}