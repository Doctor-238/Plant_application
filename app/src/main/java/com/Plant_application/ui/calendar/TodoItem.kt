package com.Plant_application.ui.calendar

import com.Plant_application.data.database.PlantItem

// 할 일의 종류를 나타내는 enum 클래스
enum class TaskType {
    WATERING, PESTICIDE
}

// 캘린더의 할 일 목록에 표시될 데이터 클래스
data class TodoItem(
    val plant: PlantItem,
    val taskType: TaskType,
    val date: Long // 할 일이 예정된 날짜 (timestamp)
)