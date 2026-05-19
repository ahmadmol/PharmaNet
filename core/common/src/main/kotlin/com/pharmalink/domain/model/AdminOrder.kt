package com.pharmalink.domain.model

data class AdminOrder(
    val id: String,
    val orderType: String,
    val status: OrderStatus,
    val medicineName: String,
    val quantity: Int,
    val unit: String,
    
    // Pharmacy Info
    val pharmacyId: String?,
    val pharmacyName: String?,
    
    // Warehouse Info (B2B only)
    val warehouseId: String?,
    val warehouseName: String?,
    
    // Customer Info (B2C only)
    val customerId: String?,
    val customerName: String?,
    
    // Urgency & Pricing
    val isUrgent: Boolean,
    val totalPriceCents: Long?,
    val currency: String,
    
    // Fulfillment
    val fulfillmentType: FulfillmentType,
    
    // Timestamps
    val createdAt: String,
    val updatedAt: String,
    val confirmedAt: String?,
    val fulfilledAt: String?,
)
