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
)

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
}

sealed interface WarehouseInventoryAction {
    data object OnRetryClicked : WarehouseInventoryAction
    data object OnRefreshTriggered : WarehouseInventoryAction
    data object OnBackClicked : WarehouseInventoryAction
    data class OnSearchQueryChanged(val query: String) : WarehouseInventoryAction
    data object OnFilterClicked : WarehouseInventoryAction
    data object OnAddMedicineClicked : WarehouseInventoryAction
    data class OnMedicineClicked(val medicineId: String) : WarehouseInventoryAction
    data class OnEditInventoryClicked(val medicineId: String) : WarehouseInventoryAction
    data class OnDeleteInventoryClicked(val medicineId: String) : WarehouseInventoryAction
}

sealed interface WarehouseInventoryEffect {
    data class ShowMessage(val message: String) : WarehouseInventoryEffect
    data object NavigateBack : WarehouseInventoryEffect
    data object NavigateToAddMedicine : WarehouseInventoryEffect
    data class NavigateToMedicineDetail(val medicineId: String) : WarehouseInventoryEffect
    data class NavigateToEditInventory(val medicineId: String) : WarehouseInventoryEffect
    data class ShowDeleteConfirmation(val medicineId: String, val medicineName: String) : WarehouseInventoryEffect
}
