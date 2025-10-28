package com.Plant_application.ui.onboarding

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Plant_application.ui.add.PlantAnalysis
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _recommendationResult = MutableLiveData<PlantAnalysis?>()
    val recommendationResult: LiveData<PlantAnalysis?> = _recommendationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var generativeModel: GenerativeModel? = null

    fun getPlantRecommendation(apiKey: String, location: String, watering: String, sunlight: String) {
        if (isLoading.value == true) return
        _isLoading.value = true

        viewModelScope.launch {
            val result = analyzePreferences(apiKey, location, watering, sunlight)
            if (result != null) {
                _recommendationResult.postValue(result)
            } else {
                _error.postValue("식물을 추천하는데 실패했어요. 다시 시도해주세요.")
            }
            _isLoading.postValue(false)
        }
    }

    private suspend fun analyzePreferences(apiKey: String, location: String, watering: String, sunlight: String): PlantAnalysis? {
        return withContext(Dispatchers.IO) {
            try {
                if (generativeModel == null) {
                    val config = GenerationConfig.Builder().apply {
                        responseMimeType = "application/json"
                    }.build()
                    generativeModel = GenerativeModel("gemini-2.5-flash", apiKey, generationConfig = config)
                }

                val prompt = """
                    You are a helpful botanist assistant. Based on the user's preferences, recommend one suitable indoor plant.
                    User preferences:
                    - Location to keep the plant: "$location"
                    - Watering preference: "$watering"
                    - Sunlight condition: "$sunlight"

                    Provide the response in a strict JSON format without any markdown.
                    The JSON response MUST contain ONLY the following keys: "is_plant", "official_name", "health_rating", "watering_cycle", "pesticide_cycle", "temp_range", "lifespan".
                    
                    - "is_plant": (boolean) Always true for a recommendation.
                    - "official_name": (string) The scientific or common official name of the recommended plant.
                    - "health_rating": (float) Set this to 5.0 as it's a new, healthy plant.
                    - "watering_cycle": (string) A recommended watering frequency for this plant.
                    - "pesticide_cycle": (string) A recommended pesticide frequency. If not needed, respond with "필요 없음".
                    - "temp_range": (string) The optimal temperature range for this plant.
                    - "lifespan": (string) The expected lifespan of this plant.
                """.trimIndent()

                val response = generativeModel!!.generateContent(prompt)
                Json { ignoreUnknownKeys = true }.decodeFromString<PlantAnalysis>(response.text!!)
            } catch (e: Exception) {
                Log.e("OnboardingViewModel", "AI recommendation failed", e)
                null
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}