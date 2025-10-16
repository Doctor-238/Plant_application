package com.Plant_application.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
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
import com.Plant_application.R
import com.Plant_application.databinding.FragmentHomeBinding
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var plantAdapter: PlantAdapter
    private var toast: Toast? = null

    // 위치 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        homeViewModel.permissionRequestedThisSession = false
        if (isGranted) {
            checkAndRefresh()
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showGoToSettingsDialog()
            } else {
                showToast("위치 권한이 거부되어 날씨 정보를 가져올 수 없습니다.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupRecyclerView()
        setupSwipeRefreshLayout()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        checkAndRefresh()
    }

    private fun setupRecyclerView() {
        plantAdapter = PlantAdapter { plant ->
            // 상세 화면으로 이동하는 Action 실행
            val action = HomeFragmentDirections.actionGlobalToAddPlantFragment() // 수정 필요
            // findNavController().navigate(action)
            // TODO: 홈에서 상세화면으로 가는 네비게이션 Action 추가 필요
        }
        binding.rvPlantList.adapter = plantAdapter
    }

    private fun setupSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            checkAndRefresh()
        }
    }

    private fun observeViewModel() {
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                homeViewModel.onErrorShown()
            }
        }

        homeViewModel.weatherInfo.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                binding.tvLocation.text = weather.name
                binding.tvWeatherDesc.text = weather.weather.firstOrNull()?.description?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
                binding.tvTemp.text = String.format(Locale.KOREAN, "%.0f°", weather.main.temp)
                binding.tvFeelsLike.text = String.format(Locale.KOREAN, "체감 %.0f°", weather.main.feels_like)
                binding.tvHumidity.text = "습도: ${weather.main.humidity}%"
            }
        }

        homeViewModel.allPlants.observe(viewLifecycleOwner) { plants ->
            binding.tvEmptyList.isVisible = plants.isEmpty()
            binding.rvPlantList.isVisible = plants.isNotEmpty()
            plantAdapter.submitList(plants)
        }
    }

    private fun checkAndRefresh() {
        if (!isAdded) return

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            showTurnOnLocationDialog()
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        homeViewModel.refreshData()
    }

    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            AlertDialog.Builder(requireContext())
                .setTitle("위치 권한 안내")
                .setMessage("현재 위치의 날씨 정보를 가져오기 위해 위치 권한이 필요합니다.")
                .setPositiveButton("권한 허용") { _, _ ->
                    homeViewModel.permissionRequestedThisSession = true
                    locationPermissionRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                .setNegativeButton("거부") { _, _ ->
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                .show()
        } else {
            homeViewModel.permissionRequestedThisSession = true
            locationPermissionRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
            .setNegativeButton("닫기") { _, _ ->
                binding.swipeRefreshLayout.isRefreshing = false
            }
            .setCancelable(false)
            .show()
    }

    private fun showTurnOnLocationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("위치 서비스 비활성화")
            .setMessage("날씨 정보를 가져오려면 위치 서비스가 필요합니다. 설정에서 위치를 켜주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showToast(message: String) {
        toast?.cancel()
        toast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toast?.cancel()
        _binding = null
    }
}