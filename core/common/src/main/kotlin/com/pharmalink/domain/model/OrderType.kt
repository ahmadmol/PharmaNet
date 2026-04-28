package com.pharmalink.domain.model

/**
 * Defines the type of order and its business context.
 */
enum class OrderType {
    /**
     * B2B: Pharmacy orders from Warehouse.
     * Requires: pharmacyId, warehouseId, requestId
     * Forbids: customerId
     */
    PHARMACY_WAREHOUSE,
    
    /**
     * B2C: Customer orders from Pharmacy.
     * Requires: customerId, pharmacyId
     * Forbids: warehouseId, requestId
     */
    CUSTOMER_PHARMACY,
}
