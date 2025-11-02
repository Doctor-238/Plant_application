package com.Plant_application.ui.add

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Plant_application.R
import com.Plant_application.data.api.WikimediaApiService
import com.Plant_application.data.api.WikimediaQuery
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.bumptech.glide.Glide
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

    private val _analysisResult = MutableLiveData<PlantAnalysis?>()
    val analysisResult: LiveData<PlantAnalysis?> = _analysisResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveComplete = MutableLiveData(false)
    val saveComplete: LiveData<Boolean> = _saveComplete

    private val _originalBitmap = MutableLiveData<Bitmap?>()
    val originalBitmap: LiveData<Bitmap?> = _originalBitmap

    private val db = AppDatabase.getDatabase(application)
    private var currentBitmap: Bitmap? = null

    private val generativeModel: GenerativeModel by lazy {
        val config = GenerationConfig.builder().apply {
            temperature = 0.4f
            topK = 32
            topP = 1.0f
            maxOutputTokens = 2048
        }.build()

        GenerativeModel(
            modelName = "gemini-2.5-flash-image",
            apiKey = getApplication<Application>().getString(R.string.gemini_api_key),
            generationConfig = config
        )
    }

    private val wikimediaApiService: WikimediaApiService by lazy {
        WikimediaApiService.create()
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 512): Bitmap {
        val originalWidth = bitmap.width; val originalHeight = bitmap.height
        var resizedWidth = originalWidth; var resizedHeight = originalHeight
        if (originalHeight > maxDimension || originalWidth > maxDimension) {
            if (originalWidth > originalHeight) {
                resizedWidth = maxDimension
                resizedHeight = (resizedWidth * originalHeight) / originalWidth
            } else {
                resizedHeight = maxDimension
                resizedWidth = (resizedHeight * originalWidth) / originalHeight
            }
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    fun resetState() {
        _isAiAnalyzing.value = false
        _analysisResult.value = null
        _originalBitmap.value = null
        currentBitmap?.recycle()
        currentBitmap = null
        _error.value = null
    }

    fun analyzePlantImage(bitmap: Bitmap) {
        if (_isAiAnalyzing.value == true) return

        resetState()

        currentBitmap = bitmap
        _originalBitmap.value = bitmap
        _isAiAnalyzing.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val resizedBitmap = resizeBitmap(bitmap)
                val result = analyzePlantWithAI(resizedBitmap)

                if (result.is_plant) {
                    _analysisResult.postValue(result)
                } else {
                    _error.postValue("식물이 아닌 것 같습니다. 다른 사진을 등록해주세요.")
                    resetState()
                }
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "AI 분석 실패", e)
                _error.postValue("AI 분석에 실패했습니다: ${e.message}")
                resetState()
            } finally {
                _isAiAnalyzing.postValue(false)
            }
        }
    }

    fun setRecommendedPlant(analysis: PlantAnalysis, context: Context) {
        _isAiAnalyzing.value = true
        _analysisResult.value = analysis

        viewModelScope.launch {
            try {
                val plantName = analysis.official_name
                if (plantName.isNullOrBlank()) {
                    throw Exception("AI가 식물 이름을 반환하지 않았습니다.")
                }

                val imageUrl = fetchImageFromWikimedia(plantName)
                if (imageUrl.isNullOrBlank()) {
                    throw Exception("Wikimedia에서 이미지를 찾지 못했습니다.")
                }

                Log.d("AddPlantViewModel", "Downloading image from URL: $imageUrl")
                val bitmap = downloadBitmap(context, imageUrl)

                if (bitmap != null) {
                    currentBitmap = bitmap
                    _originalBitmap.postValue(bitmap)
                } else {
                    throw Exception("Glide가 URL에서 이미지를 다운로드하지 못했습니다.")
                }

            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "Error loading recommended plant image", e)
                _error.postValue("추천 식물 이미지 로드 실패: ${e.message}")
                val placeholderBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.plant2)
                currentBitmap = placeholderBitmap
                _originalBitmap.postValue(placeholderBitmap)
            } finally {
                _isAiAnalyzing.postValue(false)
            }
        }
    }

    private suspend fun fetchImageFromWikimedia(plantName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = wikimediaApiService.getPageImage(titles = plantName)
                if (response.isSuccessful) {
                    val pages = response.body()?.query?.pages
                    val firstPage = pages?.values?.firstOrNull()
                    firstPage?.thumbnail?.source
                } else {
                    Log.w("AddPlantViewModel", "Wikimedia API call failed: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "fetchImageFromWikimedia failed", e)
                null
            }
        }
    }

    private suspend fun downloadBitmap(context: Context, url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "Glide download failed", e)
                null
            }
        }
    }

    private suspend fun analyzePlantWithAI(bitmap: Bitmap): PlantAnalysis = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are an expert botanist. Analyze the plant in the image and provide a detailed analysis in a strict JSON format without any markdown.
                Your JSON response MUST contain ONLY the following keys: "is_plant", "official_name", "health_rating", "watering_cycle", "pesticide_cycle", "temp_range", "lifespan".

                - "is_plant": (boolean) True if the image contains a plant, otherwise false.
                - "official_name": (string) The scientific or common official name of the plant in Korean (e.g., "몬스테라 (Monstera deliciosa)").
                - "health_rating": (float) A health score from 0.0 to 5.0. 5.0 is perfectly healthy.
                - "watering_cycle": (string) A recommended watering frequency in Korean (e.g., "주 1-2회", "10일에 한 번").
                - "pesticide_cycle": (string) A recommended pesticide frequency in Korean. If not needed, respond with "필요 없음".
                - "temp_range": (string) The optimal temperature range for this plant in Korean (e.g., "18-25°C").
                - "lifespan": (string) The expected lifespan of this plant in Korean (e.g., "수년", "10년 이상").
                
                Do NOT include 'image_url'.
                Return ONLY the JSON object.
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text ?: throw Exception("AI 응답이 비어있습니다.")

            Log.d("AddPlantViewModel", "AI 원본 응답: $responseText")
            val cleanedJson = responseText.trim().removePrefix("```json").removeSuffix("```").trim()
            Log.d("AddPlantViewModel", "정제된 JSON: $cleanedJson")

            Json { ignoreUnknownKeys = true }.decodeFromString<PlantAnalysis>(cleanedJson)

        } catch (e: Exception) {
            Log.e("AddPlantViewModel", "식물 분석 실패", e)
            throw Exception("식물 분석에 실패했습니다: ${e.message}")
        }
    }

    fun savePlantToDatabase(nickname: String) {
        if (_isSaving.value == true) return

        val analysis = _analysisResult.value
        val bitmap = currentBitmap

        if (bitmap == null) {
            _error.value = "식물 사진을 등록해주세요."
            return
        }

        if (analysis == null || !analysis.is_plant) {
            _error.value = "저장할 수 있는 식물 정보가 없습니다."
            return
        }

        _isSaving.value = true

        viewModelScope.launch {
            try {
                val imagePath = saveImageToInternalStorage(bitmap)

                val plantItem = PlantItem(
                    nickname = nickname.ifBlank { analysis.official_name ?: "이름 없는 식물" },
                    officialName = analysis.official_name ?: "알 수 없음",
                    imageUri = imagePath,
                    healthRating = analysis.health_rating ?: 3.0f,
                    wateringCycle = analysis.watering_cycle ?: "알 수 없음",
                    pesticideCycle = analysis.pesticide_cycle ?: "알 수 없음",
                    tempRange = analysis.temp_range ?: "알 수 없음",
                    lifespan = analysis.lifespan ?: "알 수 없음"
                )

                withContext(Dispatchers.IO) {
                    db.plantDao().insert(plantItem)
                }

                _saveComplete.postValue(true)

            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "저장 실패", e)
                _error.postValue("저장에 실패했습니다: ${e.message}")
            } finally {
                _isSaving.postValue(false)
            }
        }
    }

    private suspend fun saveImageToInternalStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "plant_$timeStamp.jpg"
        val file = File(getApplication<Application>().filesDir, fileName)

        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                throw IOException("비트맵 압축 실패")
            }
        }
        file.absolutePath
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        currentBitmap?.recycle()
        currentBitmap = null
    }
}