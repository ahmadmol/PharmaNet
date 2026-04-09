package com.pharmalink.feature.cart.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.R
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.domain.model.CartItem
import com.pharmalink.domain.model.CartUiState
import com.pharmalink.domain.model.DeliveryPreference
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.StockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Cart ViewModel
 * Manages cart state and business logic for cart/review screen
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialUiState())

    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private fun buildInitialUiState(): CartUiState {
        val items = getMockCartItems()
        return CartUiState(
            items = items,
            totalItems = 3,
            selectedWarehouseCount = 2,
            estimatedDeliveryTime = "2-3 أيام عمل",
            screenState = ScreenState.Success(items),
        )
    }

    fun updateItemQuantity(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedItems = currentState.items.map { item ->
                if (item.id == itemId) {
                    item.copy(quantity = newQuantity.coerceIn(1, 999))
                } else {
                    item
                }
            }

            _uiState.value = currentState.copy(
                items = updatedItems,
                totalItems = updatedItems.sumOf { it.quantity },
            )
        }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedItems = currentState.items.filter { it.id != itemId }

            _uiState.value = currentState.copy(
                items = updatedItems,
                totalItems = updatedItems.sumOf { it.quantity },
                selectedWarehouseCount = updatedItems.map { it.selectedWarehouseId }.distinct().count(),
            )
        }
    }

    fun updateRequestNotes(notes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(requestNotes = notes)
        }
    }

    fun updatePharmacyInstructions(instructions: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pharmacyInstructions = instructions)
        }
    }

    fun updatePriority(priority: RequestPriority) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedPriority = priority)
        }
    }

    fun updateDeliveryPreference(preference: DeliveryPreference) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedDeliveryPreference = preference)
        }
    }

    fun submitRequest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            kotlinx.coroutines.delay(2000)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                screenState = ScreenState.Success(emptyList()),
                items = emptyList(),
                totalItems = 0,
            )
        }
    }

    fun saveDraft() {
        viewModelScope.launch {
            // TODO: Implement draft saving
        }
    }

    private fun getMockCartItems(): List<CartItem> {
        val subtitle = context.getString(R.string.cart_demo_medicine_subtitle)
        return listOf(
            CartItem(
                id = UUID.randomUUID().toString(),
                medicineName = "أموكسيسيلين 500 مجم",
                medicineSubtitle = subtitle,
                medicineImageUrl = null,
                quantity = 2,
                unit = "عبوة",
                selectedWarehouseId = "wh1",
                selectedWarehouseName = "مستودع الرياض الرئيسي",
                selectedSupplierName = "شركة الأدوية المتحدة",
                stockStatus = StockStatus.IN_STOCK,
                isColdChain = true,
                isUrgent = false,
                isFastDelivery = true,
                notes = "يحتاج وصفة طبية",
                batchInfo = "BATCH-2024-001",
                packagingInfo = "علبة كرتون",
            ),
            CartItem(
                id = UUID.randomUUID().toString(),
                medicineName = "باراسيتامول 250 مجم",
                medicineSubtitle = subtitle,
                medicineImageUrl = null,
                quantity = 1,
                unit = "عبوة",
                selectedWarehouseId = "wh2",
                selectedWarehouseName = "مستودع جدة",
                selectedSupplierName = "شركة الأدوية الوطنية",
                stockStatus = StockStatus.LOW_STOCK,
                isColdChain = false,
                isUrgent = true,
                isFastDelivery = false,
                notes = "",
                batchInfo = "BATCH-2024-002",
                packagingInfo = "زجاجة",
            ),
            CartItem(
                id = UUID.randomUUID().toString(),
                medicineName = "إيبوبروفين 200 مجم",
                medicineSubtitle = subtitle,
                medicineImageUrl = null,
                quantity = 3,
                unit = "عبوة",
                selectedWarehouseId = "wh1",
                selectedWarehouseName = "مستودع الرياض الرئيسي",
                selectedSupplierName = "شركة الأدوية المتحدة",
                stockStatus = StockStatus.OUT_OF_STOCK,
                isColdChain = false,
                isUrgent = false,
                isFastDelivery = true,
                notes = "",
                batchInfo = null,
                packagingInfo = null,
            ),
        )
    }
}
