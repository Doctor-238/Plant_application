package com.Plant_application.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = PlantItem::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CalendarTask::class,
            parentColumns = ["id"],
            childColumns = ["linkedTaskId"],
            onDelete = ForeignKey.CASCADE // 일정이 삭제되면 일지도 함께 삭제
        )
    ],
    indices = [Index(value = ["plantId"]), Index(value = ["timestamp"]), Index(value = ["linkedTaskId"])]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Int,
    val timestamp: Long,
    val content: String,
    val linkedTaskId: Long? = null
)