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
    val wateringCycle: String,
    val pesticideCycle: String,
    val tempRange: String,
    val lifespan: String,
    val timestamp: Long = System.currentTimeMillis()
)