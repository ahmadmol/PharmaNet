package com.pharmalink.feature.warehouses

data class FeaturedWarehousesUiState(
    val warehouses: List<FeaturedWarehouseItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

enum class FeaturedDeliveryType {
    FAST,
    STANDARD,
    FLEXIBLE,
}

data class FeaturedWarehouseItem(
    val id: String,
    val name: String,
    val location: String,
    val deliveryLabel: String,
    val deliveryType: FeaturedDeliveryType,
    val inStockPercent: Int,
    val priorityRes: Int,
    val supportsColdChain: Boolean,
)
