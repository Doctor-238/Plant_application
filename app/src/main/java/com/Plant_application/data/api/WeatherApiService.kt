package com.Plant_application.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("weather")
    val weather: List<Weather>,
    @SerialName("main")
    val main: Main,
    @SerialName("name")
    val name: String // 도시 이름
)

@Serializable
data class Weather(
    @SerialName("description")
    val description: String,
    @SerialName("icon")
    val icon: String
)

@Serializable
data class Main(
    @SerialName("temp")
    val temp: Double, // 현재 온도
    @SerialName("feels_like")
    val feels_like: Double, // 체감 온도
    @SerialName("humidity")
    val humidity: Int // 습도
)