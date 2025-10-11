package com.Plant_application.data.repository

import androidx.lifecycle.LiveData
import com.Plant_application.data.database.PlantDao
import com.Plant_application.data.database.PlantItem

class PlantRepository(private val plantDao: PlantDao) {
    fun getAllPlants(): LiveData<List<PlantItem>> {
        return plantDao.getAllPlants()
    }
}