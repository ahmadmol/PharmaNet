package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    private var ordersJob: Job? = null

    init {
        loadOrders()
    }

    private fun loadOrders() {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            pharmaRepository.observeOrders()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToUserMessage(error),
                    )
                }
                .collect { orders ->
                    val currentOrders = _uiState.value.orders
                    val newOrderItems = orders.map { it.toOrderItem() }

                    if (currentOrders != newOrderItems) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            orders = newOrderItems,
                            errorMessage = null,
                        )
                    } else if (_uiState.value.isLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
        }
    }

    fun refreshOrders() {
        loadOrders()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun mapErrorToUserMessage(error: Throwable): String {
        return when {
            error.message?.contains("network", ignoreCase = true) == true ||
                error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
                error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            else -> error.message ?: context.getString(R.string.order_error_loading_failed)
        }
    }

    private fun Order.toOrderItem(): OrderItem {
        val statusText = when (status) {
            OrderStatus.PENDING -> context.getString(R.string.order_status_pending)
            OrderStatus.QUOTE_PENDING -> "بانتظار موافقة الصيدلية"
            OrderStatus.CONFIRMED -> context.getString(R.string.order_status_approved)
            OrderStatus.IN_PROGRESS -> "قيد التجهيز"
            OrderStatus.READY_FOR_PICKUP -> "جاهز للاستلام"
            OrderStatus.OUT_FOR_DELIVERY -> "قيد التوصيل"
            OrderStatus.REJECTED -> context.getString(R.string.order_status_rejected)
            OrderStatus.CANCELLED -> "ملغي"
            OrderStatus.DELIVERED -> context.getString(R.string.order_status_delivered)
        }

        val dateText = formatInstantToDisplay(createdAt)
        val lastUpdateText = formatInstantToDisplay(updatedAt)
        val priceLabel = totalPriceCents?.toCurrencyLabel(currency) ?: "بانتظار التسعير"

        return OrderItem(
            id = id,
            requestId = requestId.orEmpty(),
            orderNumber = "#$id",
            date = dateText,
            status = statusText,
            statusType = status,
            totalAmount = priceLabel,
            deliveryDate = etaLabel,
            warehouseName = warehouseName,
            warehouseId = warehouseId,
            supplierName = supplierName,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            isUrgent = isUrgent,
            lastUpdate = lastUpdateText,
            items = listOf(
                OrderItemDetail(
                    medicineName = medicineName,
                    quantity = quantity,
                    price = priceLabel,
                ),
            ),
        )
    }

    private fun Long.toCurrencyLabel(currency: String): String {
        val major = this / 100
        val minor = kotlin.math.abs(this % 100)
        return "$major.${minor.toString().padStart(2, '0')} $currency"
    }

    private fun formatInstantToDisplay(instant: java.time.Instant?): String {
        return instant?.let {
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy/MM/dd")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(it)
        } ?: "-"
    }
}
