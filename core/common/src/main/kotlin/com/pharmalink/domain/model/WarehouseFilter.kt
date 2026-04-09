package com.pharmalink.domain.model

/**
 * Warehouse Filter Enum
 */
enum class WarehouseFilter {
    ALL,
    NEARBY,
    COLD_CHAIN,
    AVAILABLE_NOW,
    SUPPLY_CHAIN,
    FAST_DELIVERY
}

/**
 * Warehouse Sort Enum
 */
enum class WarehouseSort {
    NEAREST,
    STOCK,
    LATEST,
    RATING
}
