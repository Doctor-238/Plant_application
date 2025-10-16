package com.Plant_application.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlantDao {
    @Insert
    suspend fun insert(item: PlantItem)

    @Update
    suspend fun update(item: PlantItem)

    @Delete
    suspend fun delete(item: PlantItem)

    @Query("SELECT * FROM plant_items ORDER BY timestamp DESC")
    fun getAllPlants(): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plant_items WHERE nickname LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchPlantsByName(query: String): LiveData<List<PlantItem>>

    // ID로 특정 식물 조회
    @Query("SELECT * FROM plant_items WHERE id = :id")
    fun getPlantById(id: Int): LiveData<PlantItem>
}