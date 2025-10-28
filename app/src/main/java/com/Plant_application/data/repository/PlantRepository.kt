package com.Plant_application.data.repository

import androidx.lifecycle.LiveData
import com.Plant_application.data.database.PlantDao
import com.Plant_application.data.database.PlantItem
import java.io.File

class PlantRepository(private val plantDao: PlantDao) {
    fun getAllPlants(): LiveData<List<PlantItem>> {
        return plantDao.getAllPlants()
    }

    fun searchPlantsByName(query: String): LiveData<List<PlantItem>> {
        return plantDao.searchPlantsByName(query)
    }

    // ID로 식물 조회 함수 추가
    fun getPlantById(id: Int): LiveData<PlantItem> {
        return plantDao.getPlantById(id)
    }

    suspend fun updatePlant(plant: PlantItem) {
        plantDao.update(plant)
    }

    suspend fun deletePlant(plant: PlantItem) {
        try {
            File(plant.imageUri).delete()
            plantDao.delete(plant)
        } catch (e: Exception) {
            // Log error
        }
    }
}