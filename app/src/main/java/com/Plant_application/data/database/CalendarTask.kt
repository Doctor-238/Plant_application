package com.Plant_application.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskType {
    WATERING, PESTICIDE, CUSTOM
}

@Entity(
    tableName = "calendar_tasks",
    foreignKeys = [ForeignKey(
        entity = PlantItem::class,
        parentColumns = ["id"],
        childColumns = ["plantId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["plantId"]), Index(value = ["dueDate"])]
)
data class CalendarTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Int?,
    val taskType: TaskType,
    val title: String,
    val dueDate: Long,
    var isCompleted: Boolean = false,
    var previousTimestamp: Long = 0L,
    var isSkipped: Boolean = false
)