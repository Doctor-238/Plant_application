package com.Plant_application.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CalendarTaskDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: CalendarTask): Long

    @Update
    suspend fun update(task: CalendarTask)

    @Delete
    suspend fun delete(task: CalendarTask)

    @Query("SELECT * FROM calendar_tasks WHERE dueDate = :date AND isSkipped = 0 ORDER BY isCompleted ASC, id ASC")
    fun getTasksForDate(date: Long): LiveData<List<CalendarTask>>

    @Query("SELECT * FROM calendar_tasks WHERE isCompleted = 0 AND isSkipped = 0")
    fun getIncompleteTasks(): LiveData<List<CalendarTask>>

    @Query("SELECT * FROM calendar_tasks")
    suspend fun getAllTasksList(): List<CalendarTask>

    @Query("SELECT * FROM calendar_tasks WHERE plantId = :plantId AND taskType = :taskType AND dueDate = :dueDate AND isSkipped = 0")
    suspend fun findTask(plantId: Int, taskType: TaskType, dueDate: Long): CalendarTask?

    @Query("UPDATE calendar_tasks SET isCompleted = 1 WHERE plantId = :plantId AND taskType = :taskType AND dueDate < :today AND isCompleted = 0")
    suspend fun completePastTasks(plantId: Int, taskType: TaskType, today: Long)

    @Query("DELETE FROM calendar_tasks WHERE id in (:ids)")
    suspend fun deleteTasksByIds(ids: List<Long>)

    @Query("UPDATE calendar_tasks SET isSkipped = 1 WHERE id in (:ids)")
    suspend fun skipTasksByIds(ids: List<Long>)
}