package com.pharmalink.domain.model

enum class OrderStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELIVERED,
}

data class Order(
    val id: String,
    val requestId: String,
    val medicineName: String,
    val status: OrderStatus,
    val warehouseId: String,
    val warehouseName: String,
    val supplierName: String,
    val quantity: Int,
    val unit: String,
    val createdAtLabel: String,
    val etaLabel: String?,
    val lastUpdateLabel: String,
    val isUrgent: Boolean,
)
