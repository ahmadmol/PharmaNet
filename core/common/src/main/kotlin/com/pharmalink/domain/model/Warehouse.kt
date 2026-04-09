package com.pharmalink.domain.model

data class Warehouse(
    val id: String,
    val name: String,
    val city: String,
    val district: String,
    val supportsColdChain: Boolean,
    val inStockPercent: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val estimatedDeliveryLabel: String,
    val distanceLabel: String,
    val phoneNumber: String,
    val lastUpdatedLabel: String,
)
