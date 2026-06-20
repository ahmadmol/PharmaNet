package com.pharmalink.feature.admin.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.InventoryItem
import com.pharmalink.domain.model.StockStatus as DomainStockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WarehouseInventoryViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""

    private val initialFilter: InventoryProductFilter = run {
        val raw: String? = savedStateHandle["filter"]
        when (raw) {
            "AVAILABLE" -> InventoryProductFilter.AVAILABLE
            "LOW_STOCK" -> InventoryProductFilter.LOW_STOCK
            "HIDDEN" -> InventoryProductFilter.HIDDEN
            else -> InventoryProductFilter.ALL
        }
    }

    private val _state = MutableStateFlow(WarehouseInventoryUiState(warehouseId = warehouseId, selectedFilter = initialFilter))
    val state: StateFlow<WarehouseInventoryUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<WarehouseInventoryEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<WarehouseInventoryEffect> = _effect.asSharedFlow()
    private var allMedicines: List<MedicineInventoryModel> = emptyList()

    init {
        if (warehouseId.isEmpty()) {
            _state.update { it.copy(isLoading = false, contentError = "معرف المستودع مفقود") }
        } else {
            loadInventory()
        }
    }

    fun onAction(action: WarehouseInventoryAction) {
        when (action) {
            WarehouseInventoryAction.OnRetryClicked -> loadInventory()
            WarehouseInventoryAction.OnRefreshTriggered -> refreshInventory()
            WarehouseInventoryAction.OnBackClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseInventoryEffect.NavigateBack)
                }
            }
            is WarehouseInventoryAction.OnSearchQueryChanged -> updateSearchQuery(action.query)
            is WarehouseInventoryAction.OnFilterSelected -> updateFilter(action.filter)
            WarehouseInventoryAction.OnAddMedicineClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseInventoryEffect.NavigateToAddMedicine)
                }
            }
            is WarehouseInventoryAction.OnMedicineClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseInventoryEffect.NavigateToEditMedicine(action.medicineId))
                }
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            // Load warehouse info and inventory in parallel
            val warehouseResult = pharmaRepository.getWarehouse(warehouseId)
            val inventoryResult = pharmaRepository.getWarehouseInventory(warehouseId)

            if (warehouseResult.isSuccess && inventoryResult.isSuccess) {
                val warehouse = warehouseResult.getOrNull()
                val inventory = inventoryResult.getOrNull() ?: emptyList()

                if (warehouse != null) {
                    val medicines = inventory.map { it.toUiModel() }
                    allMedicines = medicines
                    val totalItems = medicines.size
                    val capacityPercent = calculateCapacityPercent(medicines)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            warehouseName = warehouse.name,
                            totalItems = totalItems,
                            capacityPercent = capacityPercent,
                            lastUpdated = formatLastUpdated(inventory.maxOfOrNull { item -> item.lastUpdated }),
                            medicines = applyFilters(medicines, it.searchQuery, it.selectedFilter),
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = "المستودع غير موجود",
                        )
                    }
                }
            } else {
                val error = warehouseResult.exceptionOrNull() ?: inventoryResult.exceptionOrNull()
                _state.update {
                    it.copy(
                        isLoading = false,
                        contentError = error?.message ?: "فشل تحميل المخزون",
                    )
                }
            }
        }
    }

    private fun refreshInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            val warehouseResult = pharmaRepository.getWarehouse(warehouseId)
            val inventoryResult = pharmaRepository.getWarehouseInventory(warehouseId)

            if (warehouseResult.isSuccess && inventoryResult.isSuccess) {
                val warehouse = warehouseResult.getOrNull()
                val inventory = inventoryResult.getOrNull() ?: emptyList()

                if (warehouse != null) {
                    val medicines = inventory.map { it.toUiModel() }
                    allMedicines = medicines
                    val totalItems = medicines.size
                    val capacityPercent = calculateCapacityPercent(medicines)

                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            warehouseName = warehouse.name,
                            totalItems = totalItems,
                            capacityPercent = capacityPercent,
                            lastUpdated = "الآن",
                            medicines = applyFilters(medicines, it.searchQuery, it.selectedFilter),
                        )
                    }
                } else {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(WarehouseInventoryEffect.ShowMessage("فشل تحديث المخزون"))
                }
            } else {
                _state.update { it.copy(isRefreshing = false) }
                _effect.emit(WarehouseInventoryEffect.ShowMessage("فشل تحديث المخزون"))
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                medicines = applyFilters(allMedicines, query, it.selectedFilter),
            )
        }
    }

    private fun updateFilter(filter: InventoryProductFilter) {
        _state.update {
            it.copy(
                selectedFilter = filter,
                medicines = applyFilters(allMedicines, it.searchQuery, filter),
            )
        }
    }

    private fun applyFilters(
        products: List<MedicineInventoryModel>,
        query: String,
        filter: InventoryProductFilter,
    ): List<MedicineInventoryModel> {
        val normalizedQuery = query.trim()

        return products
            .asSequence()
            .filter { product ->
                when (filter) {
                    InventoryProductFilter.ALL -> true
                    InventoryProductFilter.AVAILABLE -> product.stockStatus == StockStatus.IN_STOCK
                    InventoryProductFilter.LOW_STOCK -> product.stockStatus == StockStatus.LOW_STOCK
                    InventoryProductFilter.HIDDEN -> !product.isVisible
                }
            }
            .filter { product ->
                normalizedQuery.isBlank() ||
                    product.name.contains(normalizedQuery, ignoreCase = true) ||
                    product.description.contains(normalizedQuery, ignoreCase = true) ||
                    product.priceLabel.orEmpty().contains(normalizedQuery, ignoreCase = true)
            }
            .toList()
    }

    private fun InventoryItem.toUiModel(): MedicineInventoryModel {
        return MedicineInventoryModel(
            id = id,
            medicineId = medicineId,
            name = medicineName,
            description = description.orEmpty(),
            currentQuantity = quantity,
            capacity = calculateCapacity(quantity, stockStatus),
            unit = unit,
            imageUrl = medicineImageUrl ?: "",
            priceLabel = formatOptionalPrice(priceAmount, currency),
            isVisible = isVisible,
            isActive = isActive,
            stockStatus = when (stockStatus) {
                DomainStockStatus.IN_STOCK -> StockStatus.IN_STOCK
                DomainStockStatus.LOW_STOCK -> StockStatus.LOW_STOCK
                DomainStockStatus.OUT_OF_STOCK -> StockStatus.OUT_OF_STOCK
            },
        )
    }

    private fun formatOptionalPrice(price: Double?, currency: String): String? {
        if (price == null) return null
        val amount = if (price % 1.0 == 0.0) {
            price.toLong().toString()
        } else {
            "%.2f".format(price)
        }
        return "$amount $currency"
    }

    private fun calculateCapacity(quantity: Int, status: DomainStockStatus): Int {
        // Estimate capacity based on current quantity and stock status
        return when (status) {
            DomainStockStatus.OUT_OF_STOCK -> if (quantity == 0) 100 else quantity
            DomainStockStatus.LOW_STOCK -> (quantity * 3).coerceAtLeast(100)
            DomainStockStatus.IN_STOCK -> (quantity * 1.2).toInt().coerceAtLeast(100)
        }
    }

    private fun calculateCapacityPercent(medicines: List<MedicineInventoryModel>): Int {
        if (medicines.isEmpty()) return 0
        
        val totalCurrent = medicines.sumOf { it.currentQuantity }
        val totalCapacity = medicines.sumOf { it.capacity }
        
        return if (totalCapacity > 0) {
            ((totalCurrent.toFloat() / totalCapacity) * 100).toInt()
        } else {
            0
        }
    }

    private fun formatLastUpdated(lastUpdated: Instant?): String {
        if (lastUpdated == null) return "غير متاح"
        
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(lastUpdated, now)
        
        return when {
            minutes < 1 -> "الآن"
            minutes < 60 -> "منذ $minutes دقيقة"
            minutes < 120 -> "منذ ساعة"
            minutes < 1440 -> "منذ ${minutes / 60} ساعات"
            else -> "منذ ${minutes / 1440} أيام"
        }
    }
}
