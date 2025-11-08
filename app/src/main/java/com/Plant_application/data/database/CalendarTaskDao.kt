package com.Plant_application.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CalendarTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: CalendarTask)

    @Update
    fun update(task: CalendarTask)

    @Query("DELETE FROM calendar_tasks WHERE id = :id")
    fun deleteById(id: Long)

    @Query("SELECT * FROM calendar_tasks WHERE isCompleted = 0")
    fun getIncompleteTasks(): LiveData<List<CalendarTask>>

    @Query("SELECT * FROM calendar_tasks WHERE dueDate = :date AND isCompleted = 0")
    fun getActiveTasksForDate(date: Long): LiveData<List<CalendarTask>>

    @Query("SELECT * FROM calendar_tasks WHERE dueDate = :date")
    fun getTasksForDateAll(date: Long): LiveData<List<CalendarTask>>

    @Query("UPDATE calendar_tasks SET isCompleted = 1 WHERE plantId = :plantId AND taskType = :taskType AND isCompleted = 0 AND dueDate < :today")
    fun completePastTasks(plantId: Int, taskType: TaskType, today: Long)

    @Query("DELETE FROM calendar_tasks WHERE plantId = :plantId AND taskType = :taskType AND isCompleted = 0 AND dueDate != :dueDate")
    fun purgeConflictingTasks(plantId: Int, taskType: TaskType, dueDate: Long)

    @Query("SELECT * FROM calendar_tasks WHERE plantId = :plantId AND taskType = :taskType AND isCompleted = 0 LIMIT 1")
    fun getActiveTask(plantId: Int, taskType: TaskType): CalendarTask?

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_tasks WHERE plantId = :plantId AND taskType = :taskType AND isCompleted = 0)")
    fun existActiveTask(plantId: Int, taskType: TaskType): Boolean

    @Query("SELECT * FROM plants")
    fun getAllPlantsSnapshot(): List<PlantItem>
}