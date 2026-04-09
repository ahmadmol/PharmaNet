package com.pharmalink.domain.model

/**
 * Delivery tracking domain models
 * Future-ready for maps integration
 */

data class DeliveryDelegate(
    val name: String,
    val phone: String,
    val isActive: Boolean = true,
)

enum class DeliveryStatus {
    PREPARING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVING,
    DELIVERED,
    FAILED,
}

data class DeliveryTracking(
    val orderId: String,
    val delegate: DeliveryDelegate?,
    val startPoint: String,
    val destinationPoint: String?,
    val currentStatus: DeliveryStatus,
    val departureTime: String?,
    val lastUpdate: String?,
    val orderNumber: String?,
    val estimatedArrival: String?,
    val deliveryNotes: String?,

    // Future map-ready fields (nullable, not used in current UI)
    val startLatitude: Double?,
    val startLongitude: Double?,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val driverCurrentLatitude: Double?,
    val driverCurrentLongitude: Double?,
    val routePolyline: String?,
    val lastLocationTimestamp: Long?,
)
