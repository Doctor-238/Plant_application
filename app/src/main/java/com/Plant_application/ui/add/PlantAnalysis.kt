package com.Plant_application.ui.add

import kotlinx.serialization.Serializable

@Serializable
data class PlantAnalysis(
    val is_plant: Boolean,
    val official_name: String? = null,
    val health_rating: Float? = null,
    val watering_cycle: String? = null,
    val pesticide_cycle: String? = null,
    val temp_range: String? = null,
    val lifespan: String? = null
)