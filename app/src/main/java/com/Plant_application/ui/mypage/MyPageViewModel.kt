package com.Plant_application.ui.mypage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.preference.PreferenceManager
import com.Plant_application.data.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MyPageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefs = PreferenceManager(application)
    private val plantRepository = PlantRepository(db.plantDao())

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _resetComplete = MutableLiveData(false)
    val resetComplete: LiveData<Boolean> = _resetComplete

    fun resetAllData() {
        if (_isProcessing.value == true) return
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                // 저장된 모든 식물 이미지 파일 삭제
                val allPlants = withContext(Dispatchers.IO) {
                    // LiveData가 아닌 직접 DB 접근이 필요
                    db.plantDao().getAllPlants().value ?: emptyList()
                }
                allPlants.forEach { File(it.imageUri).delete() }

                // 데이터베이스 클리어
                withContext(Dispatchers.IO) {
                    db.clearAllTables()
                }

                // SharedPreferences 초기화
                prefs.isFirstLaunch = true

                _resetComplete.postValue(true)
            } finally {
                _isProcessing.postValue(false)
            }
        }
    }
}