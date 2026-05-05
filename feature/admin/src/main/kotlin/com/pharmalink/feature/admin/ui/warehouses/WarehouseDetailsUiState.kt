package com.pharmalink.feature.admin.ui.warehouses

import androidx.compose.runtime.Immutable

@Immutable
data class WarehouseDetailsUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val warehouse: WarehouseDetailModel? = null,
)

@Immutable
data class WarehouseDetailModel(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val district: String = "",
    val phoneNumber: String = "",
    val supportsColdChain: Boolean = false,
    val inStockPercent: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val estimatedDeliveryLabel: String = "",
    val distanceLabel: String = "",
    val lastUpdatedLabel: String = "",
    val totalInventoryItems: Int = 0,
    val activeShipments: Int = 0,
    val completedOrders: Int = 0,
)

sealed interface WarehouseDetailsAction {
    data object OnRetryClicked : WarehouseDetailsAction
    data object OnManageInventoryClicked : WarehouseDetailsAction
    data object OnViewShipmentsClicked : WarehouseDetailsAction
    data object OnEditClicked : WarehouseDetailsAction
}

sealed interface WarehouseDetailsEffect {
    data class ShowMessage(val message: String) : WarehouseDetailsEffect
    data class NavigateToInventory(val warehouseId: String) : WarehouseDetailsEffect
    data class NavigateToShipments(val warehouseId: String) : WarehouseDetailsEffect
}
