package com.pharmalink.domain.model

import com.pharmalink.domain.model.AccountType

enum class RequestPriority {
    NORMAL,
    URGENT,
}

enum class RequestStatus {
    DRAFT,           // Initial state, pharmacy editing
    PENDING,         // Submitted by pharmacy, awaiting warehouse
    QUOTE_PENDING,   // Warehouse quoted price, awaiting pharmacy approval
    ACCEPTED,        // Warehouse accepted
    REJECTED,        // Warehouse rejected
    IN_PROGRESS,     // Warehouse fulfilling
    FULFILLED,       // Warehouse completed
    CANCELLED,       // Pharmacy cancelled (only from DRAFT/PENDING)
}

data class Request(
    val id: String,
    val pharmacyId: String,
    val medicineId: String? = null,
    val medicineName: String,
    val medicineSubtitle: String = "",
    val quantity: Int,
    val unit: String,
    val notes: String,
    val storageNotes: String = "",
    val totalPrice: Double = 0.0,
    val priority: RequestPriority,
    val status: RequestStatus,
    val warehouseId: String,
    val warehouseName: String,
    val supplierName: String,
    val createdAtLabel: String,
    val updatedAtLabel: String,
    val pharmacyName: String = "",
    val pharmacyPhone: String = "",
    val pharmacyLocation: String = "",
    val etaLabel: String = "",
    val relatedOrderId: String? = null,
    val rejectionReason: String? = null,
    val attachmentUrl: String? = null,
    val medicineImageUrl: String? = null,
    val items: List<RequestItem> = emptyList(),
)

/**
 * Data class for request updates.
 * Used in updateRequest() to apply partial updates with validation.
 */
data class RequestUpdate(
    val status: RequestStatus? = null,
    val warehouseId: String? = null,
    val warehouseName: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val items: List<RequestItem>? = null,
)

/**
 * Request state machine transitions.
 * Enforces valid status transitions based on user role.
 */
object RequestTransitions {

    /**
     * Checks if a status transition is valid for the given role.
     */
    fun canTransition(
        currentStatus: RequestStatus,
        targetStatus: RequestStatus,
        userRole: AccountType
    ): Boolean {
        // Same status is always allowed (no-op)
        if (currentStatus == targetStatus) return true

        return when (userRole) {
            AccountType.PHARMACY -> canPharmacyTransition(currentStatus, targetStatus)
            AccountType.WAREHOUSE -> canWarehouseTransition(currentStatus, targetStatus)
            AccountType.ADMIN -> false // ADMIN is read-only for lifecycle transitions in Phase 0D.1
            AccountType.PUBLIC_USER -> false // PUBLIC_USER cannot transition
        }
    }

    private fun canPharmacyTransition(
        current: RequestStatus,
        target: RequestStatus
    ): Boolean {
        return when (current to target) {
            // Pharmacy can submit draft requests
            RequestStatus.DRAFT to RequestStatus.PENDING -> true
            // Pharmacy can cancel draft or pending requests
            RequestStatus.DRAFT to RequestStatus.CANCELLED -> true
            RequestStatus.PENDING to RequestStatus.CANCELLED -> true
            RequestStatus.QUOTE_PENDING to RequestStatus.ACCEPTED -> true
            RequestStatus.QUOTE_PENDING to RequestStatus.REJECTED -> true
            // No other transitions allowed for pharmacy
            else -> false
        }
    }

    private fun canWarehouseTransition(
        current: RequestStatus,
        target: RequestStatus
    ): Boolean {
        return when (current to target) {
            // Warehouse can accept or reject pending requests
            RequestStatus.PENDING to RequestStatus.QUOTE_PENDING -> true
            RequestStatus.PENDING to RequestStatus.ACCEPTED -> true
            RequestStatus.PENDING to RequestStatus.REJECTED -> true
            // Warehouse can move accepted to in-progress
            RequestStatus.ACCEPTED to RequestStatus.IN_PROGRESS -> true
            // Warehouse can fulfill in-progress requests
            RequestStatus.IN_PROGRESS to RequestStatus.FULFILLED -> true
            // No other transitions allowed for warehouse
            else -> false
        }
    }

    /**
     * Gets the list of valid next statuses for a given role and current status.
     */
    fun getValidNextStatuses(
        currentStatus: RequestStatus,
        userRole: AccountType
    ): List<RequestStatus> {
        return RequestStatus.entries.filter { targetStatus ->
            canTransition(currentStatus, targetStatus, userRole)
        }
    }
}
