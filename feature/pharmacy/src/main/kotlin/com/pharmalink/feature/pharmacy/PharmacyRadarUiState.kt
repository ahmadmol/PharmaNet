package com.pharmalink.feature.pharmacy

import com.pharmalink.data.dto.NearbyOrderDto

data class PharmacyRadarUiState(
    val isLoading: Boolean = false,
    val currentLocationName: String = "",
    val nearbyOrders: List<NearbyOrderDto> = emptyList(),
    val errorMessage: String? = null,
    val isLocationMissing: Boolean = false,
) {
    val nearbyOrdersCount: Int
        get() = nearbyOrders.size
}