package com.pharmalink.domain.model

/**
 * Lightweight projection of a B2B request for ADMIN list views.
 *
 * Returned by the admin_get_all_requests RPC. Includes joined
 * pharmacy_name / warehouse_name for display without extra round-trips.
 */
data class AdminRequest(
    val id: String,
    val status: RequestStatus,
    val medicineId: String?,
    val medicineName: String,
    val medicineSubtitle: String?,
    val quantity: Int,
    val unit: String,
    val pharmacyId: String?,
    val pharmacyName: String?,
    val warehouseId: String?,
    val warehouseName: String?,
    val relatedOrderId: String?,
    val priority: String?,
    val rejectionReason: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
)
