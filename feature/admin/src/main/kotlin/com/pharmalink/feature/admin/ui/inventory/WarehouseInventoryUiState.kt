package com.pharmalink.feature.admin.ui.inventory

import androidx.compose.runtime.Immutable

@Immutable
data class WarehouseInventoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: String = "",
    val warehouseId: String = "",
    val warehouseName: String = "",
    val totalItems: Int = 0,
    val capacityPercent: Int = 0,
    val lastUpdated: String = "",
    val searchQuery: String = "",
    val selectedFilter: InventoryProductFilter = InventoryProductFilter.ALL,
    val medicines: List<MedicineInventoryModel> = emptyList(),
)

@Immutable
data class MedicineInventoryModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currentQuantity: Int = 0,
    val capacity: Int = 0,
    val unit: String = "علبة",
    val imageUrl: String = "",
    val stockStatus: StockStatus = StockStatus.IN_STOCK,
    val priceLabel: String? = null,
    val isVisible: Boolean = true,
    val isActive: Boolean = true,
)

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
}

enum class InventoryProductFilter {
    ALL,
    AVAILABLE,
    LOW_STOCK,
    HIDDEN,
}

sealed interface WarehouseInventoryAction {
    data object OnRetryClicked : WarehouseInventoryAction
    data object OnRefreshTriggered : WarehouseInventoryAction
    data object OnBackClicked : WarehouseInventoryAction
    data class OnSearchQueryChanged(val query: String) : WarehouseInventoryAction
    data class OnFilterSelected(val filter: InventoryProductFilter) : WarehouseInventoryAction
    data object OnAddMedicineClicked : WarehouseInventoryAction
}

sealed interface WarehouseInventoryEffect {
    data class ShowMessage(val message: String) : WarehouseInventoryEffect
    data object NavigateBack : WarehouseInventoryEffect
    data object NavigateToAddMedicine : WarehouseInventoryEffect
}
