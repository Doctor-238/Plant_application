package com.Plant_application.ui.home

data class DailyWeatherSummary(
    val locationName: String,
    val currentTemp: Double,
    val maxTemp: Double,
    val minTemp: Double,
    val humidity: Int,
    val weatherCondition: String,
    val weatherIcon: String
)