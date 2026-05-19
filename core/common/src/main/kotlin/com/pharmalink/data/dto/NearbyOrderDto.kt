package com.pharmalink.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyOrderDto(
    val id: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("medicine_name") val medicineName: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
)
