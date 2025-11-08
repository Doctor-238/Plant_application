package com.Plant_application.data.repository

import androidx.lifecycle.LiveData
import com.Plant_application.data.database.PlantDao
import com.Plant_application.data.database.PlantItem
import java.io.File

class PlantRepository(private val plantDao: PlantDao) {

    fun getPlants(query: String, sortType: String): LiveData<List<PlantItem>> {
        return when (sortType) {
            "이름 오름차순" -> plantDao.getPlantsOrderByNameAsc(query)
            "이름 내림차순" -> plantDao.getPlantsOrderByNameDesc(query)
            "오래된 순" -> plantDao.getPlantsOrderByOldest(query)
            else -> plantDao.getPlantsOrderByRecent(query)
        }
    }

    fun getAllPlants(): LiveData<List<PlantItem>> {
        return plantDao.getAllPlants()
    }

    fun searchPlantsByName(query: String): LiveData<List<PlantItem>> {
        return plantDao.searchPlantsByName(query)
    }

    fun getPlantById(id: Int): LiveData<PlantItem> {
        return plantDao.getPlantById(id)
    }

    suspend fun getPlantByIdSnapshot(id: Int): PlantItem? {
        return plantDao.getPlantByIdSnapshot(id)
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