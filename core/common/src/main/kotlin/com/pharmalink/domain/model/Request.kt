package com.pharmalink.domain.model

enum class RequestPriority {
    NORMAL,
    URGENT,
}

enum class RequestStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    COMPLETED,
    REJECTED,
}

data class Request(
    val id: String,
    val medicineName: String,
    val medicineSubtitle: String = "",
    val quantity: Int,
    val unit: String,
    val notes: String,
    val storageNotes: String = "",
    val priority: RequestPriority,
    val status: RequestStatus,
    val warehouseId: String,
    val warehouseName: String,
    val supplierName: String,
    val createdAtLabel: String,
    val updatedAtLabel: String,
    val etaLabel: String = "",
    val relatedOrderId: String? = null,
    val attachmentUrl: String? = null,
    val medicineImageUrl: String? = null,
)
