package com.Plant_application.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.repository.PlantRepository
import kotlinx.coroutines.launch

class EditPlantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlantRepository

    private val _plantId = MutableLiveData<Int>()
    val plantItemFromDb: LiveData<PlantItem>

    private var originalPlantItem: PlantItem? = null
    private val _currentPlantItem = MutableLiveData<PlantItem?>()
    val currentPlantItem: LiveData<PlantItem?> = _currentPlantItem

    private val _isChanged = MutableLiveData(false)
    val isChanged: LiveData<Boolean> = _isChanged

    val canBeSaved = MediatorLiveData<Boolean>().apply {
        addSource(_isChanged) { value = checkCanBeSaved() }
        addSource(_currentPlantItem) { value = checkCanBeSaved() }
    }

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _isSaveComplete = MutableLiveData(false)
    val isSaveComplete: LiveData<Boolean> = _isSaveComplete

    private val _isDeleteComplete = MutableLiveData(false)
    val isDeleteComplete: LiveData<Boolean> = _isDeleteComplete

    init {
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        repository = PlantRepository(plantDao)
        plantItemFromDb = _plantId.switchMap { id ->
            repository.getPlantById(id)
        }
    }

    private fun checkCanBeSaved(): Boolean {
        val hasChanges = _isChanged.value ?: false
        val isNicknameValid = !_currentPlantItem.value?.nickname.isNullOrBlank()
        return hasChanges && isNicknameValid
    }

    fun loadPlant(id: Int) {
        if (_plantId.value == id) return
        _plantId.value = id
    }

    fun setInitialState(item: PlantItem) {
        if (originalPlantItem == null || originalPlantItem?.id != item.id) {
            originalPlantItem = item.copy()
            _currentPlantItem.value = item.copy()
            _isChanged.value = false
        }
    }

    fun updateNickname(nickname: String) {
        _currentPlantItem.value?.let {
            if (it.nickname != nickname) {
                it.nickname = nickname
                _currentPlantItem.postValue(it)
                checkForChanges()
            }
        }
    }

    private fun checkForChanges() {
        _isChanged.value = originalPlantItem != _currentPlantItem.value
    }

    fun saveChanges() {
        if (_isProcessing.value == true || canBeSaved.value != true) return

        _currentPlantItem.value?.let {
            _isProcessing.value = true
            viewModelScope.launch {
                try {
                    repository.updatePlant(it)
                    _isSaveComplete.postValue(true)
                } finally {
                    _isProcessing.postValue(false)
                }
            }
        }
    }

    fun deletePlant() {
        if (_isProcessing.value == true) return
        _currentPlantItem.value?.let { itemToDelete ->
            _isProcessing.value = true
            viewModelScope.launch {
                try {
                    repository.deletePlant(itemToDelete)
                    _isDeleteComplete.postValue(true)
                } finally {
                    _isProcessing.postValue(false)
                }
            }
        }
    }
}