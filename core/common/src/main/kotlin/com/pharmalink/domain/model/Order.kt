package com.pharmalink.domain.model

import java.time.Instant

/**
 * Extended order status for both B2B (Pharmacy-Warehouse) and B2C (Customer-Pharmacy) flows.
 */
enum class OrderStatus {
    /** Order created, awaiting seller confirmation with price */
    PENDING,
    
    /** Seller accepted order, price set */
    CONFIRMED,
    
    /** Seller rejected order */
    REJECTED,
    
    /** B2B: Warehouse is processing */
    IN_PROGRESS,
    
    /** B2C PICKUP: Order ready at pharmacy */
    READY_FOR_PICKUP,
    
    /** B2C DELIVERY: Out for delivery */
    OUT_FOR_DELIVERY,
    
    /** Order completed */
    DELIVERED,
    
    /** Cancelled by buyer (before CONFIRMED) or seller */
    CANCELLED,
}

enum class CustomerRequestUrgency {
    URGENT,
    NORMAL,
}

enum class CustomerRequestScope {
    SPECIFIC_PHARMACY,
    ALL_PHARMACIES,
}

/**
 * Order model supporting both B2B (Pharmacy ↔ Warehouse) and B2C (Customer ↔ Pharmacy) flows.
 * 
 * Invariants:
 * - PHARMACY_WAREHOUSE: requires pharmacyId, warehouseId, requestId; customerId must be null
 * - CUSTOMER_PHARMACY: requires customerId, pharmacyId; warehouseId and requestId must be null
 * - totalPriceCents is null during PENDING, required from CONFIRMED onwards
 * - deliveryAddress/Phone required only for B2C DELIVERY
 */
data class Order(
    val id: String,
    
    // === Core Fields ===
    /** Medicine identifier - source of truth */
    val medicineId: String,
    
    /** Denormalized medicine name for display */
    val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: OrderStatus,
    val orderType: OrderType,
    val fulfillmentType: FulfillmentType,
    
    // === Ownership Fields ===
    /**
     * Always present:
     * - B2B: The buying pharmacy
     * - B2C: The selling pharmacy (fulfillment location)
     */
    val pharmacyId: String?,
    
    /** B2B: Supplier warehouse | B2C: null */
    val warehouseId: String?,
    
    /** B2C: Buying customer | B2B: null */
    val customerId: String?,
    
    /** B2B: Linked request | B2C: null */
    val requestId: String?,
    
    // === Financial ===
    /** Final approved price in cents (null during PENDING) */
    val totalPriceCents: Long?,
    val currency: String = "SAR",
    
    // === Conditional Fields ===
    /** Required for B2C DELIVERY, null for PICKUP */
    val deliveryAddress: String?,
    
    /** Required for B2C DELIVERY, null for PICKUP */
    val deliveryPhone: String?,
    
    /** Customer notes (B2C) or warehouse notes (B2B) */
    val notes: String?,
    
    // === Timestamps ===
    val createdAt: Instant,
    val updatedAt: Instant,
    val confirmedAt: Instant?,    // When seller confirmed with price
    val fulfilledAt: Instant?,    // When order completed
    
    // === Legacy Fields (B2B backward compatibility) ===
    val warehouseName: String? = null,
    val supplierName: String? = null,
    val etaLabel: String? = null,
    val isUrgent: Boolean = false,
    val urgency: CustomerRequestUrgency = if (isUrgent) CustomerRequestUrgency.URGENT else CustomerRequestUrgency.NORMAL,
    val requestScope: CustomerRequestScope = CustomerRequestScope.SPECIFIC_PHARMACY,
    val pharmacyName: String? = null,
    val pharmacyLocation: String? = null,
) {
    companion object {
        /**
         * Validates that order invariants are satisfied.
         * Returns true if valid, false otherwise.
         */
        fun validateInvariants(order: Order): Boolean {
            return when (order.orderType) {
                OrderType.PHARMACY_WAREHOUSE -> validateB2BInvariants(order)
                OrderType.CUSTOMER_PHARMACY -> validateB2CInvariants(order)
            }
        }
        
        private fun validateB2BInvariants(order: Order): Boolean {
            // Required fields
            if (order.pharmacyId.isNullOrBlank()) return false
            if (order.warehouseId.isNullOrBlank()) return false
            if (order.requestId.isNullOrBlank()) return false
            
            // Forbidden fields
            if (order.customerId != null) return false
            if (!order.deliveryAddress.isNullOrBlank()) return false // B2B uses pharmacy address
            
            // B2B always uses DELIVERY fulfillment type
            if (order.fulfillmentType != FulfillmentType.DELIVERY) return false
            
            return true
        }
        
        private fun validateB2CInvariants(order: Order): Boolean {
            // Required fields
            if (order.customerId.isNullOrBlank()) return false
            if (order.requestScope == CustomerRequestScope.SPECIFIC_PHARMACY && order.pharmacyId.isNullOrBlank()) return false
            
            // Forbidden fields
            if (order.warehouseId != null) return false
            if (order.requestId != null) return false
            
            // Conditional fields based on fulfillment type
            when (order.fulfillmentType) {
                FulfillmentType.DELIVERY -> {
                    if (order.deliveryAddress.isNullOrBlank()) return false
                    if (order.deliveryPhone.isNullOrBlank()) return false
                }
                FulfillmentType.PICKUP -> {
                    // deliveryAddress and deliveryPhone are optional for PICKUP
                }
            }
            
            return true
        }
        
        /**
         * Validates price invariant: null during PENDING, required from CONFIRMED onwards.
         */
        fun validatePriceInvariant(status: OrderStatus, totalPriceCents: Long?): Boolean {
            return when (status) {
                OrderStatus.PENDING -> totalPriceCents == null
                OrderStatus.CONFIRMED,
                OrderStatus.IN_PROGRESS,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED -> totalPriceCents != null && totalPriceCents >= 0
                OrderStatus.REJECTED,
                OrderStatus.CANCELLED -> true // Price may or may not be set
            }
        }
        
        /**
         * Checks if a status transition is valid.
         */
        fun isValidTransition(from: OrderStatus, to: OrderStatus, orderType: OrderType, isSeller: Boolean): Boolean {
            return when (from) {
                OrderStatus.PENDING -> when (to) {
                    OrderStatus.CONFIRMED -> isSeller
                    OrderStatus.REJECTED -> isSeller
                    OrderStatus.CANCELLED -> !isSeller // Buyer can cancel
                    else -> false
                }
                OrderStatus.CONFIRMED -> when (to) {
                    OrderStatus.IN_PROGRESS -> orderType == OrderType.PHARMACY_WAREHOUSE && isSeller
                    OrderStatus.READY_FOR_PICKUP -> orderType == OrderType.CUSTOMER_PHARMACY && isSeller
                    OrderStatus.OUT_FOR_DELIVERY -> orderType == OrderType.CUSTOMER_PHARMACY && isSeller
                    OrderStatus.REJECTED -> isSeller
                    else -> false
                }
                OrderStatus.IN_PROGRESS -> when (to) {
                    OrderStatus.DELIVERED -> isSeller
                    else -> false
                }
                OrderStatus.READY_FOR_PICKUP -> when (to) {
                    OrderStatus.DELIVERED -> isSeller // Pharmacy marks as picked up
                    else -> false
                }
                OrderStatus.OUT_FOR_DELIVERY -> when (to) {
                    OrderStatus.DELIVERED -> isSeller
                    else -> false
                }
                OrderStatus.DELIVERED,
                OrderStatus.REJECTED,
                OrderStatus.CANCELLED -> false // Terminal states
            }
        }
    }
}
