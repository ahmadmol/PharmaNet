package com.pharmalink.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbyOrderDto(
    val id: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("medicine_name") val medicineName: String? = null,

    /**
     * Distance can be hidden by hardened RPC/RLS; keep nullable to avoid deserialization crashes.
     */
    @SerialName("distance_km") val distanceKm: Double? = null,

    /**
     * Optional metadata that may be missing/hidden depending on server/RLS/RPC contract.
     * Keep nullable for safe parsing.
     */
    @SerialName("urgency_status") val urgencyStatus: String? = null,
)
