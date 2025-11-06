package com.Plant_application.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var nickname: String,
    val officialName: String,
    val imageUri: String,
    val healthRating: Float,

    val wateringCycleMin: Int,
    val wateringCycleMax: Int,
    val pesticideCycleMin: Int,
    val pesticideCycleMax: Int,
    val tempRange: String,
    val lifespanMin: Int,
    val lifespanMax: Int,
    val estimatedAge: Int,

    val lastWateredTimestamp: Long = 0L,
    val lastPesticideTimestamp: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)