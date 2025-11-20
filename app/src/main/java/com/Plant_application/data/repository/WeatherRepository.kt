package com.Plant_application.data.repository

import com.Plant_application.data.api.WeatherApiService
import com.Plant_application.data.api.WeatherResponse
import retrofit2.Response

class WeatherRepository(private val weatherApiService: WeatherApiService) {
    suspend fun getFiveDayForecast(lat: Double, lon: Double, apiKey: String): Response<WeatherResponse> {
        return weatherApiService.getFiveDayForecast(lat, lon, apiKey)
    }
}