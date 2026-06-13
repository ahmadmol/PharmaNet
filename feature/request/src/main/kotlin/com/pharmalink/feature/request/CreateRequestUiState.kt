package com.pharmalink.feature.request

import com.pharmalink.domain.model.Medicine

data class CreateRequestUiState(
    val medicines: List<MedicineItem> = emptyList(),
    val warehouses: List<WarehouseOption> = emptyList(),
    val items: List<CreateRequestBasketItem> = emptyList(),
    val selectedMedicine: MedicineItem? = null,
    val quantity: String = "1",
    val pendingUnit: String = "",
    val notes: String = "",
    val selectedWarehouseId: String = "",
    val selectedWarehouseName: String = "",
    val isSuccess: Boolean = false,
    val createdRequestId: String = "",
    val isUrgent: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMedicines: Boolean = false,
    val medicineLoadError: String? = null,
    val errorMessage: String? = null
)

data class CreateRequestBasketItem(
    val medicineId: String,
    val medicineName: String,
    val medicineSubtitle: String,
    val quantity: Int,
    val unit: String,
    val warehouseId: String = "",
    val warehouseName: String = "",
)

data class MedicineItem(
    val id: String,
    val name: String,
    val brand: String,
    val strength: String,
    val price: Double,
    val warehouseId: String = "",
)

fun Medicine.toItem(): MedicineItem = MedicineItem(
    id = id,
    name = name,
    brand = brand,
    strength = strength,
    price = price,
    warehouseId = warehouseId.orEmpty(),
)


data class WarehouseOption(
    val id: String,
    val name: String,
    val location: String,
    val statusLabel: String,
    val deliveryLabel: String,
    val stockPercent: Int,
)
