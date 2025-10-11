package com.Plant_application.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlantDao {
    @Insert
    suspend fun insert(item: PlantItem)

    @Update
    suspend fun update(item: PlantItem)

    @Query("SELECT * FROM plant_items ORDER BY timestamp DESC")
    fun getAllPlants(): LiveData<List<PlantItem>>

}