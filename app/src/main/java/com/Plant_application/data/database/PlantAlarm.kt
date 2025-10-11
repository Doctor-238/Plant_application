package com.Plant_application.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "plant_alarms",
    foreignKeys = [ForeignKey(
        entity = PlantItem::class,
        parentColumns = ["id"],
        childColumns = ["plantId"],
        onDelete = ForeignKey.CASCADE // 식물이 삭제되면 관련 알람도 함께 삭제
    )]
)
data class PlantAlarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val plantId: Int, // 어떤 식물에 대한 알람인지
    val alarmType: String, // "물 주기", "살충제" 등
    val hour: Int, // 시
    val minute: Int, // 분
    val intervalDays: Int, // 반복 주기 (일)
    var isEnabled: Boolean = true // 알람 On/Off 여부
)