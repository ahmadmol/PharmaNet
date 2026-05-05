package com.pharmalink.domain.model

data class CreateFacilityRequest(
    val type: FacilityType,
    val name: String,
    val address: String,
    val phone: String,
    val licenseNumber: String,
    val latitude: Double?,
    val longitude: Double?,
    val isActive: Boolean,
)
