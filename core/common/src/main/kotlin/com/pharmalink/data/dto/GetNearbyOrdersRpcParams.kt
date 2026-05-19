package com.pharmalink.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetNearbyOrdersRpcParams(
    @SerialName("p_latitude") val latitude: Double,
    @SerialName("p_longitude") val longitude: Double,
    @SerialName("p_radius_km") val radiusKm: Double,
)

