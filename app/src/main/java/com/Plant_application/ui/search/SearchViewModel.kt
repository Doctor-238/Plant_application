package com.Plant_application.ui.search

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
import com.Plant_application.data.api.WikimediaApiService
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.data.repository.WeatherRepository
import com.Plant_application.ui.add.PlantAnalysis
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

@SuppressLint("MissingPermission")
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: PreferenceManager
    private val weatherRepository: WeatherRepository
    private val wikimediaApiService: WikimediaApiService by lazy { WikimediaApiService.create() }
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var cancellationTokenSource = CancellationTokenSource()

    private val _isLoadingRecommendations = MutableLiveData(false)
    val isLoadingRecommendations: LiveData<Boolean> = _isLoadingRecommendations

    private val _recommendationError = MutableLiveData<String?>(null)
    val recommendationError: LiveData<String?> = _recommendationError

    private val _surveyRecommendation = MutableLiveData<PlantAnalysis?>(null)
    val surveyRecommendation: LiveData<PlantAnalysis?> = _surveyRecommendation

    private val _weatherRecommendation = MutableLiveData<PlantAnalysis?>(null)
    val weatherRecommendation: LiveData<PlantAnalysis?> = _weatherRecommendation

    private val _surveyRecommendationImage = MutableLiveData<String?>()
    val surveyRecommendationImage: LiveData<String?> = _surveyRecommendationImage

    private val _weatherRecommendationImage = MutableLiveData<String?>()
    val weatherRecommendationImage: LiveData<String?> = _weatherRecommendationImage

    private var generativeModel: GenerativeModel? = null

    init {
        prefs = PreferenceManager(application)
        weatherRepository = WeatherRepository(WeatherApiService.create())
    }

    fun loadRecommendations() {
        if (_isLoadingRecommendations.value == true) return
        _isLoadingRecommendations.value = true
        _recommendationError.value = null

        viewModelScope.launch {
            val surveyJob = async(Dispatchers.IO) {
                try {
                    val space = prefs.surveySpace
                    val sunlight = prefs.surveySunlight
                    val temp = prefs.surveyTemp
                    val humidity = prefs.surveyHumidity
                    if (space == -1 || sunlight == -1 || temp == -1 || humidity == -1) {
                        throw Exception("설문조사 정보가 없습니다. 앱 재설치 후 초기 설문을 진행해주세요.")
                    }
                    val rec = runAiRecommendation(space, sunlight, temp, humidity)
                    _surveyRecommendation.postValue(rec)
                    rec?.official_name?.let {
                        val imageUrl = fetchImageFromWikimedia(it)
                        _surveyRecommendationImage.postValue(imageUrl)
                    }
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Survey rec failed", e)
                    _recommendationError.postValue("설문 기반 추천 실패: ${e.message}")
                }
            }

            val weatherJob = async(Dispatchers.IO) {
                try {
                    val location = getFreshLocation()
                    val weather = fetchWeather(location)
                    val currentForecast = weather.list.firstOrNull()
                        ?: throw IOException("날씨 예보 정보가 없습니다.")

                    val tempRating = tempToRating(currentForecast.main.temp)
                    val humidityRating = humidityToRating(currentForecast.main.humidity)

                    val space = prefs.surveySpace.takeIf { it != -1 } ?: 3
                    val sunlight = prefs.surveySunlight.takeIf { it != -1 } ?: 3

                    val rec = runAiRecommendation(space, sunlight, tempRating, humidityRating)
                    _weatherRecommendation.postValue(rec)
                    rec?.official_name?.let {
                        val imageUrl = fetchImageFromWikimedia(it)
                        _weatherRecommendationImage.postValue(imageUrl)
                    }
                } catch (e: Exception) {
                    Log.e("SearchViewModel", "Weather rec failed", e)
                    val errorMsg = when (e) {
                        is SecurityException -> "위치 권한이 없어 날씨 추천을 받지 못했습니다."
                        else -> "날씨 기반 추천 실패: ${e.message}"
                    }
                    _recommendationError.postValue(errorMsg)
                }
            }

            surveyJob.await()
            weatherJob.await()
            _isLoadingRecommendations.postValue(false)
        }
    }

    private suspend fun getFreshLocation(): Location {
        if (ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("No location permission")
        }
        cancellationTokenSource.cancel()
        cancellationTokenSource = CancellationTokenSource()
        return fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token
        ).await()
    }

    private suspend fun fetchWeather(location: Location): WeatherResponse {
        val apiKey = getApplication<Application>().getString(R.string.openweathermap_api_key)
        val response = weatherRepository.getFiveDayForecast(location.latitude, location.longitude, apiKey)
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw IOException("Failed to fetch weather: ${response.code()}")
        }
    }

    private fun tempToRating(temp: Double): Int {
        return when {
            temp < 15 -> 1
            temp < 20 -> 2
            temp < 25 -> 3
            temp < 30 -> 4
            else -> 5
        }
    }

    private fun humidityToRating(humidity: Int): Int {
        return when {
            humidity < 30 -> 1
            humidity < 45 -> 2
            humidity < 60 -> 3
            humidity < 75 -> 4
            else -> 5
        }
    }

    private suspend fun runAiRecommendation(
        space: Int,
        sunlight: Int,
        temperature: Int,
        humidity: Int
    ): PlantAnalysis? {
        return withContext(Dispatchers.IO) {
            try {
                if (generativeModel == null) {
                    val config = GenerationConfig.Builder().apply {
                        responseMimeType = "application/json"
                    }.build()

                    generativeModel = GenerativeModel(
                        "gemini-2.5-flash-lite",
                        getApplication<Application>().getString(R.string.gemini_api_key),
                        generationConfig = config
                    )
                }

                val prompt = """
                    You are a helpful botanist assistant. Based on the user's environmental ratings (1-5 scale), recommend one suitable indoor plant.
                    User preferences:
                    - Space (1=Small indoor, 5=Large outdoor): $space
                    - Sunlight (1=Low, 5=High): $sunlight
                    - Temperature (1=Low, 5=High): $temperature
                    - Humidity (1=Low, 5=High): $humidity

                    Provide the response in a strict JSON format without any markdown.
                    The JSON response MUST contain ONLY the following keys: 
                    "is_plant", "official_name", "health_rating", "temp_range", 
                    "watering_cycle_min_days", "watering_cycle_max_days", 
                    "pesticide_cycle_min_days", "pesticide_cycle_max_days", 
                    "lifespan_min_years", "lifespan_max_years", "estimated_age_days".
                    
                    - "is_plant": (boolean) Always true for a recommendation.
                    - "official_name": (string) The **exact Korean Wikipedia page title** for the recommended plant (e.g., "몬스테라", "스파티필룸"). Do NOT include latin names or parentheses.
                    - "health_rating": (float) Set this to 5.0 as it's a new, healthy plant.
                    - "temp_range": (string) The optimal temperature range for this plant in Korean (e.g., "18-25°C").
                    - "watering_cycle_min_days": (int) The minimum recommended days between watering (e.g., 4).
                    - "watering_cycle_max_days": (int) The maximum recommended days between watering (e.g., 7).
                    - "pesticide_cycle_min_days": (int) The minimum recommended days between pesticide use (e.g., 30).
                    - "pesticide_cycle_max_days": (int) The maximum recommended days between pesticide use (e.g., 60). If not needed, respond with 0 for both min and max.
                    - "lifespan_min_years": (int) The minimum expected lifespan in years (e.g., 5).
                    - "lifespan_max_years": (int) The maximum expected lifespan in years (e.g., 10).
                    - "estimated_age_days": (int) Set this to 0, as it's a new recommendation.
                    
                    Do NOT include 'image_url' or any other keys.
                    Ensure the response is ONLY the JSON object.
                """.trimIndent()

                val response = generativeModel!!.generateContent(prompt)
                val cleanedJson = response.text!!.trim().removePrefix("```json").removeSuffix("```").trim()
                Json { ignoreUnknownKeys = true }.decodeFromString<PlantAnalysis>(cleanedJson)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "AI recommendation failed", e)
                throw e
            }
        }
    }

    private suspend fun fetchImageFromWikimedia(plantName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val summaryResponse = wikimediaApiService.getPageSummary(title = plantName)
                if (summaryResponse.isSuccessful) {
                    val imageUrl = summaryResponse.body()?.thumbnail?.source
                    if (imageUrl != null) return@withContext imageUrl
                }

                Log.w("SearchViewModel", "getPageSummary failed. Falling back to search API.")
                val searchResponse = wikimediaApiService.searchPages(srsearch = plantName)
                if (searchResponse.isSuccessful) {
                    val firstTitle = searchResponse.body()?.query?.search?.firstOrNull()?.title

                    if (firstTitle != null) {
                        val retryResponse = wikimediaApiService.getPageSummary(title = firstTitle)
                        if (retryResponse.isSuccessful) {
                            return@withContext retryResponse.body()?.thumbnail?.source
                        }
                    }
                }
                Log.w("SearchViewModel", "All Wikimedia image fetch attempts failed.")
                null

            } catch (e: Exception) {
                Log.e("SearchViewModel", "fetchImageFromWikimedia failed", e)
                null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancellationTokenSource.cancel()
    }
}