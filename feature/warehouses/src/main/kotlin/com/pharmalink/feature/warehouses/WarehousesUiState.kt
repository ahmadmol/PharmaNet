package com.pharmalink.feature.warehouses

data class WarehousesUiState(
    val warehouses: List<WarehouseItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class WarehouseItem(
    val id: String,
    val name: String,
    val address: String,
    val status: String,
    val statusType: StatusType,
    val supportsColdChain: Boolean = false,
    val stockPercent: Int = 0,
    val distance: String = "",
    val estimatedDelivery: String = "",
    val phoneNumber: String = "",
    val lastUpdated: String = ""
)

enum class StatusType {
    AVAILABLE,
    LOW_STOCK,
    CLOSED
}
