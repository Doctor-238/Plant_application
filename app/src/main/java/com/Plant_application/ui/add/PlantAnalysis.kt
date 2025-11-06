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
    val temp_range: String? = null,

    val watering_cycle_min_days: Int? = null,
    val watering_cycle_max_days: Int? = null,
    val pesticide_cycle_min_days: Int? = null,
    val pesticide_cycle_max_days: Int? = null,
    val lifespan_min_years: Int? = null,
    val lifespan_max_years: Int? = null,
    val estimated_age_days: Int? = null
) : Parcelable