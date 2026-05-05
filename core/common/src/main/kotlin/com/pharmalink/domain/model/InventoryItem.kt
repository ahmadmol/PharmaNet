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
)
