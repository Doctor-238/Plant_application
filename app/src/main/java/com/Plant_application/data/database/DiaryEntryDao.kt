package com.Plant_application.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DiaryEntryDao {
    @Insert
    suspend fun insert(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getEntriesForPlant(plantId: Int): LiveData<List<DiaryEntry>>

    @Query("DELETE FROM diary_entries WHERE linkedTaskId = :taskId")
    suspend fun deleteByLinkedTaskId(taskId: Long)

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("DELETE FROM diary_entries WHERE plantId = :plantId AND content = :content AND timestamp = :timestamp")
    suspend fun deleteManualEntry(plantId: Int, content: String, timestamp: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM diary_entries WHERE linkedTaskId = :taskId LIMIT 1)")
    suspend fun existsByTaskId(taskId: Long): Boolean
}