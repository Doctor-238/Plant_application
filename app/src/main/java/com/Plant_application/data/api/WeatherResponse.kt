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
    var name: String
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
    val temp: Double,
    @SerialName("feels_like")
    val feels_like: Double,
    @SerialName("humidity")
    val humidity: Int
)