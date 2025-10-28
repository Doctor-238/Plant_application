package com.Plant_application.ui.add

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Plant_application.R
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlantViewModel(application: Application) : AndroidViewModel(application) {

    private val _isAiAnalyzing = MutableLiveData(false)
    val isAiAnalyzing: LiveData<Boolean> = _isAiAnalyzing

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _isSaveCompleted = MutableLiveData(false)
    val isSaveCompleted: LiveData<Boolean> = _isSaveCompleted

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _originalBitmap = MutableLiveData<Bitmap?>()
    val originalBitmap: LiveData<Bitmap?> = _originalBitmap

    private val _analysisResult = MutableLiveData<PlantAnalysis?>()
    val analysisResult: LiveData<PlantAnalysis?> = _analysisResult

    val hasChanges = MutableLiveData(false)

    private var generativeModel: GenerativeModel? = null

    fun onImageSelected(bitmap: Bitmap, apiKey: String) {
        resetAllState()
        _originalBitmap.value = bitmap
        hasChanges.value = true

        val config = GenerationConfig.Builder().apply {
            responseMimeType = "application/json"
        }.build()

        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = config
        )

        _isAiAnalyzing.value = true

        viewModelScope.launch {
            val result = analyzeImage(bitmap)
            _isAiAnalyzing.postValue(false)

            if (result == null || !result.is_plant) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "식물 사진이 아니거나, 분석에 실패했습니다."
                    resetAllState()
                }
            } else {
                _analysisResult.postValue(result)
            }
        }
    }

    fun setInitialAnalysis(analysis: PlantAnalysis) {
        if (_analysisResult.value == null) {
            _analysisResult.value = analysis
            hasChanges.value = true // 변경 사항이 있음을 알림
        }
    }

    private suspend fun analyzeImage(bitmap: Bitmap): PlantAnalysis? {
        return withContext(Dispatchers.IO) {
            try {
                // AI 프롬프트를 요구사항에 맞게 상세하고 명확하게 수정
                val inputContent = content {
                    image(bitmap)
                    text("""
                        You are an expert botanist. Analyze the plant in the image and provide a detailed analysis in a strict JSON format without any markdown.
                        Your JSON response MUST contain ONLY the following keys: "is_plant", "official_name", "health_rating", "watering_cycle", "pesticide_cycle", "temp_range", "lifespan".

                        - "is_plant": (boolean) True if the image contains a plant, otherwise false.
                        - "official_name": (string) The scientific or common official name of the plant.
                        - "health_rating": (float) A health score from 0.0 to 5.0. 5.0 is perfectly healthy.
                        - "watering_cycle": (string) A recommended watering frequency (e.g., "주 1-2회", "10일에 한 번").
                        - "pesticide_cycle": (string) A recommended pesticide frequency. If not needed, respond with "필요 없음".
                        - "temp_range": (string) The optimal temperature range for the plant (e.g., "18°C ~ 25°C").
                        - "lifespan": (string) The expected lifespan of the plant (e.g., "약 5년", "10년 이상").
                    """.trimIndent())
                }

                val response = generativeModel!!.generateContent(inputContent)
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<PlantAnalysis>(response.text!!)
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "AI analysis failed", e)
                null
            }
        }
    }

    fun savePlant(nickname: String) {
        viewModelScope.launch {
            val analysis = _analysisResult.value
            val bitmap = _originalBitmap.value
            if (analysis == null || bitmap == null) {
                _errorMessage.value = "분석 결과가 없거나 이미지가 없습니다."
                return@launch
            }

            _isSaving.value = true
            try {
                val imagePath = saveBitmapToInternalStorage(bitmap)
                if (imagePath == null) {
                    throw IOException("이미지 저장 실패")
                }

                val newPlant = PlantItem(
                    nickname = nickname,
                    officialName = analysis.official_name ?: "N/A",
                    imageUri = imagePath,
                    wateringCycle = analysis.watering_cycle ?: "N/A",
                    pesticideCycle = analysis.pesticide_cycle ?: "N/A",
                    healthRating = analysis.health_rating ?: 0.0f,
                    tempRange = analysis.temp_range ?: "N/A",
                    lifespan = analysis.lifespan ?: "N/A"
                )

                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(getApplication()).plantDao().insert(newPlant)
                }
                _isSaveCompleted.postValue(true)

            } catch (e: Exception) {
                _errorMessage.postValue("저장 중 오류 발생: ${e.message}")
            } finally {
                _isSaving.postValue(false)
            }
        }
    }

    private suspend fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            val directory = getApplication<Application>().filesDir
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "plant_$timeStamp.jpg"
            val file = File(directory, fileName)
            try {
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
                file.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun resetAllState() {
        _originalBitmap.value = null
        _analysisResult.value = null
        _isAiAnalyzing.value = false
        _isSaving.value = false
        _isSaveCompleted.value = false
        hasChanges.value = false
    }
}