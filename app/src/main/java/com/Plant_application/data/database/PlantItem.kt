package com.Plant_application.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_items")
data class PlantItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var nickname: String, // 사용자가 설정하는 별명
    val officialName: String, // AI가 알려준 공식 이름
    val imageUri: String,
    val wateringCycle: String, // 물 주기
    val pesticideCycle: String, // 살충제 주기
    val healthRating: Float, // 건강 여부 (별점 0.0 ~ 5.0)
    val tempRange: String, // 적정 온도 범위
    val lifespan: String, // 수명
    val timestamp: Long = System.currentTimeMillis()
)