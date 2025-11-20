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

    @Query("SELECT * FROM plants WHERE (:query = '' OR nickname LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun getPlantsOrderByRecent(query: String): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE (:query = '' OR nickname LIKE '%' || :query || '%') ORDER BY timestamp ASC")
    fun getPlantsOrderByOldest(query: String): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE (:query = '' OR nickname LIKE '%' || :query || '%') ORDER BY nickname ASC")
    fun getPlantsOrderByNameAsc(query: String): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE (:query = '' OR nickname LIKE '%' || :query || '%') ORDER BY nickname DESC")
    fun getPlantsOrderByNameDesc(query: String): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun getPlantById(id: Int): LiveData<PlantItem>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantByIdSnapshot(id: Int): PlantItem?

    @Query("SELECT * FROM plants")
    fun getAllPlants(): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE nickname LIKE '%' || :query || '%'")
    fun searchPlantsByName(query: String): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants")
    suspend fun getAllPlantsList(): List<PlantItem>

    @Query("DELETE FROM plants")
    suspend fun clearAll()

    @Query("SELECT * FROM plants WHERE needsAttentionTimestamp IS NOT NULL ORDER BY needsAttentionTimestamp ASC")
    fun getNeedsAttentionPlants(): LiveData<List<PlantItem>>

    @Query("SELECT * FROM plants WHERE needsAttentionTimestamp IS NOT NULL ORDER BY needsAttentionTimestamp ASC")
    suspend fun getNeedsAttentionPlantsSnapshot(): List<PlantItem>
}