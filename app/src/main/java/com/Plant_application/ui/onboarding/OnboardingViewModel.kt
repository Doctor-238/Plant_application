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

    fun getPlantRecommendation(
        apiKey: String,
        space: Int,
        sunlight: Int,
        temperature: Int,
        humidity: Int
    ) {
        if (isLoading.value == true) return
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = analyzePreferences(apiKey, space, sunlight, temperature, humidity)
            if (result != null) {
                _recommendationResult.postValue(result)
            } else {
                _error.postValue("식물을 추천하는데 실패했어요. 다시 시도해주세요.")
            }
            _isLoading.postValue(false)
        }
    }

    private suspend fun analyzePreferences(
        apiKey: String,
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
                        "gemini-2.5-flash",
                        apiKey,
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
                    The JSON response MUST contain ONLY the following keys: "is_plant", "official_name", "health_rating", "watering_cycle", "pesticide_cycle", "temp_range", "lifespan".
                    
                    - "is_plant": (boolean) Always true for a recommendation.
                    - "official_name": (string) The **exact Korean Wikipedia page title** for the recommended plant (e.g., "몬스테라", "스파티필룸"). Do NOT include latin names or parentheses.
                    - "health_rating": (float) Set this to 5.0 as it's a new, healthy plant.
                    - "watering_cycle": (string) A recommended watering frequency for this plant in Korean.
                    - "pesticide_cycle": (string) A recommended pesticide frequency in Korean. If not needed, respond with "필요 없음".
                    - "temp_range": (string) The optimal temperature range for this plant in Korean.
                    - "lifespan": (string) The expected lifespan of this plant in Korean.
                    
                    Do NOT include 'image_url'.
                    Ensure the response is ONLY the JSON object.
                """.trimIndent()

                val response = generativeModel!!.generateContent(prompt)
                val cleanedJson = response.text!!.trim().removePrefix("```json").removeSuffix("```").trim()
                Json { ignoreUnknownKeys = true }.decodeFromString<PlantAnalysis>(cleanedJson)
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