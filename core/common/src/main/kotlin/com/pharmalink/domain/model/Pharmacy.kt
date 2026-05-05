package com.pharmalink.domain.model

data class Pharmacy(
    val id: String,
    val name: String,
    val location: String?,
    val contactNumber: String?,
    val licenseNumber: String?,
    val isActive: Boolean,
    val createdAt: String
)

enum class PublicPharmacyAvailabilityStatus {
    AVAILABLE,
    NEEDS_CONFIRMATION,
    UNKNOWN,
}

data class PublicPharmacyForMedicine(
    val pharmacyId: String,
    val pharmacyName: String,
    val location: String,
    val area: String?,
    val city: String?,
    val district: String?,
    val supportsDelivery: Boolean,
    val supportsPickup: Boolean,
    val isOnDuty: Boolean,
    val distanceLabel: String?,
    val availabilityStatus: PublicPharmacyAvailabilityStatus,
    val estimatedTimeLabel: String?,
)
