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
import androidx.navigation.fragment.findNavController
import com.Plant_application.R
import com.Plant_application.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var plantAdapter: PlantAdapter
    private var toast: Toast? = null
    private var locationDialog: AlertDialog? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        homeViewModel.permissionRequestedThisSession = false
        if (isGranted) {
            checkAndRefresh()
        } else {
            homeViewModel.stopLoading()
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

        binding.ivSettings.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_settings)
        }

        binding.ivAddPlantHome.setOnClickListener {
            findNavController().navigate(R.id.action_global_addPlantFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRefresh()
    }

    private fun setupRecyclerView() {
        plantAdapter = PlantAdapter { plant ->
            val action = HomeFragmentDirections.actionNavigationHomeToPlantDetailFragment(plant.id)
            findNavController().navigate(action)
        }
        binding.rvPlants.adapter = plantAdapter
    }

    private fun setupSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            checkAndRefresh()
        }
    }

    private fun observeViewModel() {
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (binding.swipeRefreshLayout.isRefreshing != isLoading) {
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        homeViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                homeViewModel.onErrorShown()
            }
        }

        homeViewModel.weatherInfo.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                binding.cardWeather.visibility = View.VISIBLE
                binding.tvLocation.text = weather.name

                val weatherCondition = weather.weather.firstOrNull()
                if (weatherCondition != null) {
                    val description = weatherCondition.description.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    binding.tvWeatherDesc.text = description
                    binding.ivWeatherIcon.setImageResource(getWeatherIcon(weatherCondition.icon))
                }

                val sdf = SimpleDateFormat("yyyy.MM.dd EEEE HH:mm", Locale.KOREAN)
                binding.tvDateTime.text = sdf.format(Date())

                binding.tvTemp.text = String.format(Locale.KOREAN, "%.0f°", weather.main.temp)
                binding.tvHumidity.text = "${weather.main.humidity}%"
            } else {
                binding.cardWeather.visibility = View.INVISIBLE
            }
        }

        homeViewModel.allPlants.observe(viewLifecycleOwner) { plants ->
            binding.tvEmptyList.isVisible = plants.isEmpty()
            binding.rvPlants.isVisible = plants.isNotEmpty()
            plantAdapter.submitList(plants)
        }
    }

    private fun getWeatherIcon(iconCode: String): Int {
        return when (iconCode.dropLast(1)) {
            "01" -> R.drawable.sunny
            "02" -> R.drawable.cloudy
            "03", "04" -> R.drawable.cloudy
            "09", "10" -> R.drawable.rainy
            "11" -> R.drawable.thunder
            "13" -> R.drawable.snowy
            "50" -> R.drawable.foggy
            else -> R.drawable.cloudy
        }
    }

    private fun checkAndRefresh() {
        if (!isAdded) return

        homeViewModel.startLoading()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            homeViewModel.stopLoading()
            showTurnOnLocationDialog()
            return
        }

        val apiKey = getString(R.string.openweathermap_api_key)
        if (apiKey.isBlank() || apiKey == "YOUR_OPENWEATHERMAP_API_KEY") {
            showToast("날씨 API 키가 설정되지 않았습니다.")
            homeViewModel.stopLoading()
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
                    homeViewModel.stopLoading()
                }
                .setOnDismissListener {
                    if (binding.swipeRefreshLayout.isRefreshing){
                        homeViewModel.stopLoading()
                    }
                }
                .show()
        } else {
            homeViewModel.permissionRequestedThisSession = true
            locationPermissionRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun showGoToSettingsDialog() {
        if (locationDialog != null && locationDialog!!.isShowing) return

        locationDialog = AlertDialog.Builder(requireContext())
            .setTitle("권한 필요")
            .setMessage("날씨 정보를 위해 위치 권한이 반드시 필요합니다. '설정'으로 이동하여 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("닫기") { _, _ ->
                homeViewModel.stopLoading()
            }
            .setCancelable(false)
            .show()
    }

    private fun showTurnOnLocationDialog() {
        if (locationDialog != null && locationDialog!!.isShowing) return

        locationDialog = AlertDialog.Builder(requireContext())
            .setTitle("위치 서비스 비활성화")
            .setMessage("날씨 정보를 가져오려면 위치 서비스가 필요합니다. 설정에서 위치를 켜주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("취소") { _, _ ->
                homeViewModel.stopLoading()
            }
            .setOnDismissListener {
                homeViewModel.stopLoading()
            }
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
        locationDialog?.dismiss()
        locationDialog = null
        _binding = null
    }
}