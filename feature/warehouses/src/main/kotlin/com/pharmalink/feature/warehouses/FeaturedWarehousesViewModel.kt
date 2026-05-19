package com.pharmalink.feature.warehouses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FeaturedWarehousesViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeaturedWarehousesUiState(isLoading = true))
    val uiState: StateFlow<FeaturedWarehousesUiState> = _uiState.asStateFlow()

    init {
        loadFeaturedWarehouses()
    }

    fun refreshFeaturedWarehouses() {
        loadFeaturedWarehouses()
    }

    private fun loadFeaturedWarehouses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = pharmaRepository.fetchFeaturedWarehouses()
            result
                .onSuccess { warehouses ->
                    _uiState.value = FeaturedWarehousesUiState(
                        warehouses = warehouses.map { it.toFeaturedWarehouseItem() },
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToUserMessage(error),
                    )
                }
        }
    }

    private fun mapErrorToUserMessage(error: Throwable): String =
        when {
            error.message?.contains("network", ignoreCase = true) == true ||
                error.message?.contains("connection", ignoreCase = true) == true -> {
                context.getString(R.string.error_network)
            }

            error.message?.contains("permission", ignoreCase = true) == true ||
                error.message?.contains("unauthorized", ignoreCase = true) == true -> {
                context.getString(R.string.error_permission)
            }

            else -> {
                error.message ?: context.getString(R.string.featured_error_loading_failed)
            }
        }

    private fun Warehouse.toFeaturedWarehouseItem(): FeaturedWarehouseItem {
        val priorityRes = when {
            inStockPercent >= 90 -> R.string.featured_priority_premium
            inStockPercent >= 75 -> R.string.featured_priority_fast
            else -> R.string.featured_priority_reliable
        }

        val resolvedLocation = listOf(city, district)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { context.getString(R.string.featured_location_fallback) }

        val resolvedDelivery = estimatedDeliveryLabel.ifBlank {
            context.getString(R.string.featured_delivery_fallback)
        }

        return FeaturedWarehouseItem(
            id = id,
            name = name,
            location = resolvedLocation,
            deliveryLabel = resolvedDelivery,
            deliveryType = estimatedDeliveryLabel.toFeaturedDeliveryType(),
            inStockPercent = inStockPercent.coerceIn(0, 100),
            priorityRes = priorityRes,
            supportsColdChain = supportsColdChain,
        )
    }

    private fun String.toFeaturedDeliveryType(): FeaturedDeliveryType {
        val normalized = trim().lowercase()
        return when {
            normalized.contains("same") ||
                normalized.contains("hour") ||
                normalized.contains("fast") ||
                normalized.contains(context.getString(R.string.delivery_keyword_fast)) ||
                normalized.contains(context.getString(R.string.delivery_keyword_same_day)) ||
                normalized.contains(context.getString(R.string.delivery_keyword_hour_sa)) -> FeaturedDeliveryType.FAST

            normalized.isBlank() -> FeaturedDeliveryType.FLEXIBLE
            else -> FeaturedDeliveryType.STANDARD
        }
    }
}
