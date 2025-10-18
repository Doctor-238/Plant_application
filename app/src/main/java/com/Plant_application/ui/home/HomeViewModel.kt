package com.Plant_application.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
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
import com.Plant_application.data.repository.PlantRepository
import com.Plant_application.data.repository.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

@SuppressLint("MissingPermission")
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherRepository: WeatherRepository
    private val plantRepository: PlantRepository

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
    }

    fun refreshData() {
        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    throw SecurityException("위치 권한이 없습니다.")
                }
                val location = getFreshLocation()
                fetchWeatherForLocation(location, getApplication<Application>().getString(R.string.openweathermap_api_key))

            } catch (e: Exception) {
                handleFetchError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getFreshLocation(): Location {
        return fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).await()
    }

    private suspend fun fetchWeatherForLocation(location: Location, apiKey: String) {
        val response = weatherRepository.getCurrentWeather(location.latitude, location.longitude, apiKey)
        if (response.isSuccessful && response.body() != null) {
            _weatherInfo.postValue(response.body())
        } else {
            val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
            throw IOException("날씨 정보를 가져오는 데 실패했습니다: $errorBody")
        }
    }

    private fun handleFetchError(e: Exception) {
        Log.e("HomeViewModel", "데이터 가져오기 오류", e)
        val errorMessage = when (e) {
            is SecurityException -> "날씨 정보를 보려면 위치 권한을 허용해주세요."
            is IOException -> e.message
            else -> "위치를 가져올 수 없습니다. GPS를 켜고 잠시 후 다시 시도해주세요."
        }
        _error.postValue(errorMessage)
    }

    fun onErrorShown() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cancellationTokenSource.cancel()
    }
}
