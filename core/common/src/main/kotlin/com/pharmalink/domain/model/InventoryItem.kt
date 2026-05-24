package com.pharmalink.domain.model

import java.time.Instant

data class InventoryItem(
    val id: String,
    val medicineId: String,
    val medicineName: String,
    val medicineImageUrl: String?,
    val warehouseId: String,
    val quantity: Int,
    val unit: String,
    val stockStatus: StockStatus,
    val lastUpdated: Instant,
    val description: String? = null,
    val priceAmount: Double? = null,
    val currency: String = "SYP",
    val isVisible: Boolean = true,
    val isActive: Boolean = true,
)
