package com.pharmalink.domain.model

data class WarehouseShipment(
    val id: String,
    val warehouseId: String,
    val title: String,
    val etaLabel: String,
    val statusLabel: String,
    val itemsCount: Int,
)
