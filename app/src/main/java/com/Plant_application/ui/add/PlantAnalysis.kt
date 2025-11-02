package com.Plant_application.ui.add

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PlantAnalysis(
    val is_plant: Boolean,
    val official_name: String? = null,
    val health_rating: Float? = null,
    val watering_cycle: String? = null,
    val pesticide_cycle: String? = null,
    val temp_range: String? = null,
    val lifespan: String? = null,
    val image_url: String? = null
) : Parcelable