package com.pharmalink.feature.admin.ui.warehouses

import androidx.compose.runtime.Immutable

@Immutable
data class AdminWarehousesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: String = "",
    val warehouses: List<WarehouseItemModel> = emptyList(),
    val searchQuery: String = "",
    val sortBy: WarehouseSortBy = WarehouseSortBy.NAME,
    val totalCapacityPercent: Int = 0,
    // Note: activeShipments removed - endpoint not available yet
)

enum class WarehouseSortBy {
    NAME,
    LOCATION,
    DATE_ADDED
}

@Immutable
data class WarehouseItemModel(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val isActive: Boolean = false,
    val temperature: String = "",
    val inventoryCount: Int = 0,
    val lastUpdatedLabel: String = "",
)

sealed interface AdminWarehousesAction {
    data object OnRetryClicked : AdminWarehousesAction
    data object OnRefreshTriggered : AdminWarehousesAction
    data object OnMenuClicked : AdminWarehousesAction
    data class OnSearchQueryChanged(val query: String) : AdminWarehousesAction
    data class OnSortByChanged(val sortBy: WarehouseSortBy) : AdminWarehousesAction
    data class OnWarehouseClicked(val warehouseId: String) : AdminWarehousesAction
    data class OnManageInventoryClicked(val warehouseId: String) : AdminWarehousesAction
    data object OnAddWarehouseClicked : AdminWarehousesAction
}

sealed interface AdminWarehousesEffect {
    data class ShowMessage(val message: String) : AdminWarehousesEffect
    data object ShowAdminMenu : AdminWarehousesEffect
    data class NavigateToWarehouseDetail(val warehouseId: String) : AdminWarehousesEffect
    data class NavigateToInventoryManagement(val warehouseId: String) : AdminWarehousesEffect
}
