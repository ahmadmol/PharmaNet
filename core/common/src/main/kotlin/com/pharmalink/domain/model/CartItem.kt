package com.pharmalink.domain.model

import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus

/**
 * Cart Item Data Model
 * Represents a medicine item in the cart before request submission
 */
data class CartItem(
    val id: String,
    val medicineName: String,
    val medicineSubtitle: String = "",
    val medicineImageUrl: String? = null,
    val quantity: Int,
    val unit: String,
    val selectedWarehouseId: String,
    val selectedWarehouseName: String,
    val selectedSupplierName: String,
    val stockStatus: StockStatus,
    val isColdChain: Boolean = false,
    val isUrgent: Boolean = false,
    val isFastDelivery: Boolean = false,
    val notes: String = "",
    val batchInfo: String? = null,
    val packagingInfo: String? = null,
)

/**
 * Stock Status for cart items
 */
enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

/**
 * Cart UI State
 */
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val totalItems: Int = 0,
    val selectedWarehouseCount: Int = 0,
    val estimatedDeliveryTime: String? = null,
    val requestNotes: String = "",
    val pharmacyInstructions: String = "",
    val selectedPriority: RequestPriority = RequestPriority.NORMAL,
    val selectedDeliveryPreference: DeliveryPreference = DeliveryPreference.STANDARD,
    val isLoading: Boolean = false,
    val screenState: ScreenState<List<CartItem>> = ScreenState.Empty,
)

/**
 * Delivery Preferences
 */
enum class DeliveryPreference {
    STANDARD,
    EXPRESS,
    SCHEDULED
}
