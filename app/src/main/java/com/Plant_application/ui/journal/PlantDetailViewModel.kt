package com.Plant_application.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.Plant_application.data.database.AppDatabase
import com.Plant_application.data.database.PlantItem
import com.Plant_application.data.repository.PlantRepository

class PlantDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PlantRepository
    private val _plantId = MutableLiveData<Int>()

    val plantItem: LiveData<PlantItem>

    init {
        val plantDao = AppDatabase.getDatabase(application).plantDao()
        repository = PlantRepository(plantDao)

        plantItem = _plantId.switchMap { id ->
            repository.getPlantById(id)
        }
    }

    fun loadPlant(id: Int) {
        if (_plantId.value == id) return
        _plantId.value = id
    }
}