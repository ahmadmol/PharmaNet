package com.pharmalink.domain.model

import java.time.Instant

data class PharmacyCustomerOrder(
    val id: String,
    val customerId: String?,
    val customerName: String?,
    val medicineId: String,
    val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: OrderStatus,
    val fulfillmentType: FulfillmentType,
    val deliveryAddress: String?,
    val deliveryPhone: String?,
    val prescriptionUrl: String? = null,
    val notes: String?,
    val totalPriceCents: Long?,
    val currency: String,
    val urgency: CustomerRequestUrgency,
    val requestScope: CustomerRequestScope,
    val createdAt: Instant,
    val updatedAt: Instant,
)
