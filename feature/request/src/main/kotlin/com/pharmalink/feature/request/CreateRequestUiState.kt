package com.pharmalink.feature.request

import com.pharmalink.domain.model.Medicine

data class CreateRequestUiState(
    val medicines: List<MedicineItem> = emptyList(),
    val warehouses: List<WarehouseOption> = emptyList(),
    val selectedMedicine: MedicineItem? = null,
    val quantity: String = "1",
    val notes: String = "",
    val selectedWarehouseId: String = "",
    val selectedWarehouseName: String = "",
    val isSuccess: Boolean = false,
    val createdRequestId: String = "",
    val isLoading: Boolean = false,
    val isLoadingMedicines: Boolean = false,
    val medicineLoadError: String? = null,
    val errorMessage: String? = null
)

data class MedicineItem(
    val id: String,
    val name: String,
    val brand: String,
    val strength: String,
    val price: Double
)

fun Medicine.toItem(): MedicineItem = MedicineItem(
    id = id,
    name = name,
    brand = brand,
    strength = strength,
    price = price
)


data class WarehouseOption(
    val id: String,
    val name: String,
    val location: String,
    val statusLabel: String,
    val deliveryLabel: String,
    val stockPercent: Int,
)
