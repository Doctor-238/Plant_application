package com.Plant_application.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Plant_application.R
import com.Plant_application.data.api.WeatherApiService
import com.Plant_application.data.api.WeatherResponse
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.data.repository.PlantRepository
import com.Plant_application.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

@SuppressLint("MissingPermission")
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherRepository: WeatherRepository
    private val plantRepository: PlantRepository
    private val prefs: PreferenceManager
    private val geocoder = Geocoder(application, Locale.KOREAN)

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var fetchJob: Job? = null
    private var cancellationTokenSource = CancellationTokenSource()

    private val _weatherInfo = MutableLiveData<WeatherResponse?>()
    val weatherInfo: LiveData<WeatherResponse?> = _weatherInfo

    val allPlants: LiveData<List<PlantItem>>

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    var permissionRequestedThisSession = false

    init {
        weatherRepository = WeatherRepository(WeatherApiService.create())
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        plantRepository = PlantRepository(plantDao)
        allPlants = plantRepository.getAllPlants()
        prefs = PreferenceManager(application)
    }

    fun startLoading() {
        if (_isLoading.value != true) _isLoading.value = true
    }

    fun stopLoading() {
        _isLoading.value = false
    }

    fun refreshData() {
        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            startLoading()
            try {
                if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    throw SecurityException("위치 권한이 없습니다.")
                    stopLoading()
                }

                var locationToFetch: Location?
                try {
                    // 1. 새로운 위치 가져오기 시도
                    val freshLocation = getFreshLocation()
                    // 성공 시: 위치 저장 및 해당 위치로 날씨 가져오기
                    prefs.lastKnownLat = freshLocation.latitude.toFloat()
                    prefs.lastKnownLon = freshLocation.longitude.toFloat()
                    locationToFetch = freshLocation
                } catch (locationError: Exception) {
                    // 2. 실패 시: 캐시된 위치 불러오기
                    val cachedLat = prefs.lastKnownLat
                    val cachedLon = prefs.lastKnownLon

                    if (cachedLat != 0f && cachedLon != 0f) {
                        // 캐시된 위치가 있으면
                        locationToFetch = Location("cached").apply {
                            latitude = cachedLat.toDouble()
                            longitude = cachedLon.toDouble()
                        }
                    } else {
                        // 3. 캐시된 위치도 없으면, 원래 발생한 오류를 다시 던짐
                        throw locationError
                    }
                }

                // 4. 위치가 있으면 (새롭거나 캐시된 위치) 날씨 가져오기
                val apiKey = getApplication<Application>().getString(R.string.openweathermap_api_key)
                fetchWeatherForLocation(locationToFetch!!, apiKey)

            } catch (e: Exception) {
                // 5. 위치 가져오기(캐시 포함) 또는 날씨 가져오기 실패 시 오류 처리
                handleFetchError(e)
            } finally {
                stopLoading()
            }
        }
    }

    private suspend fun getFreshLocation(): Location {
        try {
            // 1. 마지막으로 알려진 위치 (빠름)
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < 600000) {
                return lastLocation
            }
        } catch (e: Exception) {
            Log.w("HomeViewModel", "마지막 위치 가져오기 실패", e)
        }

        // 2. 현재 위치 (높은 정확도 - GPS 우선)
        return fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).await()
    }

    @Suppress("DEPRECATION")
    private suspend fun getAddressFromLocation(location: Location): String? = withContext(Dispatchers.IO) {
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses.isNullOrEmpty()) {
                return@withContext null
            }
            val address = addresses[0]
            val locality = address.locality
            val subLocality = address.subLocality

            when {
                !locality.isNullOrBlank() && !subLocality.isNullOrBlank() -> "$locality, $subLocality"
                !locality.isNullOrBlank() -> locality
                !subLocality.isNullOrBlank() -> subLocality
                else -> address.adminArea
            }
        } catch (e: IOException) {
            Log.e("HomeViewModel", "Geocoder 실패", e)
            null
        }
    }


    private suspend fun fetchWeatherForLocation(location: Location, apiKey: String) {
        val response = weatherRepository.getCurrentWeather(location.latitude, location.longitude, apiKey)
        if (response.isSuccessful && response.body() != null) {
            val weatherData = response.body()!!
            val addressName = getAddressFromLocation(location)

            if (!addressName.isNullOrBlank()) {
                weatherData.name = addressName
            }

            _weatherInfo.postValue(weatherData)
        } else {
            val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
            throw IOException("날씨 정보를 가져오는 데 실패했습니다: ${response.code()} $errorBody")
        }
    }

    private fun handleFetchError(e: Exception) {
        Log.e("HomeViewModel", "데이터 가져오기 오류", e)
        val errorMessage = when (e) {
            is SecurityException -> "날씨 정보를 보려면 위치 권한을 허용해주세요."
            is IOException -> "날씨 정보를 가져올 수 없습니다. 인터넷 연결을 확인해주세요."
            is com.google.android.gms.tasks.RuntimeExecutionException -> "위치를 가져올 수 없습니다. GPS를 켜고 잠시 후 다시 시도해주세요."
            else -> return // NPE 등 기타 오류는 무시
        }
        _error.postValue(errorMessage)
    }

    fun onErrorShown() {
        _error.value = null
    }

    fun updateLastWatered(plant: PlantItem) {
        viewModelScope.launch {
            val updatedPlant = plant.copy(lastWateredTimestamp = System.currentTimeMillis())
            plantRepository.updatePlant(updatedPlant)
        }
    }

    fun updateLastPesticide(plant: PlantItem) {
        viewModelScope.launch {
            val updatedPlant = plant.copy(lastPesticideTimestamp = System.currentTimeMillis())
            plantRepository.updatePlant(updatedPlant)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancellationTokenSource.cancel()
    }
}