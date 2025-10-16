package com.Plant_application.ui.add

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize // Parcelable 구현을 위한 어노테이션
@Serializable
data class PlantAnalysis(
    val is_plant: Boolean,
    val official_name: String? = null,
    val health_rating: Float? = null,
    val watering_cycle: String? = null,
    val pesticide_cycle: String? = null,
    val temp_range: String? = null,
    val lifespan: String? = null
) : Parcelable // Parcelable 인터페이스 구현