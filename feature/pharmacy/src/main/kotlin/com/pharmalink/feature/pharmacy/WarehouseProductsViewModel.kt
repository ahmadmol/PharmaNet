package com.pharmalink.feature.pharmacy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WarehouseProductsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""
    private val allProducts = mutableListOf<WarehouseProductUiModel>()

    private val _uiState = MutableStateFlow(WarehouseProductsUiState())
    val uiState: StateFlow<WarehouseProductsUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun retry() {
        loadProducts()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                products = filterProducts(query),
            )
        }
    }

    private fun loadProducts() {
        if (warehouseId.isBlank()) {
            _uiState.value = WarehouseProductsUiState(
                isLoading = false,
                errorMessage = "تعذر تحديد المستودع",
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            pharmaRepository.getWarehouseProducts(warehouseId).fold(
                onSuccess = { medicines ->
                    allProducts.clear()
                    allProducts.addAll(
                        medicines
                            .filter { medicine -> medicine.isVisible && medicine.isActive }
                            .map { medicine -> medicine.toUiModel() },
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            products = filterProducts(it.searchQuery),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "تعذر تحميل منتجات المستودع",
                        )
                    }
                },
            )
        }
    }

    private fun filterProducts(query: String): List<WarehouseProductUiModel> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return allProducts.toList()

        return allProducts.filter { product ->
            product.name.contains(normalizedQuery, ignoreCase = true) ||
                product.subtitle.contains(normalizedQuery, ignoreCase = true) ||
                product.priceLabel.orEmpty().contains(normalizedQuery, ignoreCase = true)
        }
    }

    private fun Medicine.toUiModel(): WarehouseProductUiModel =
        WarehouseProductUiModel(
            id = id,
            name = name,
            subtitle = description?.takeIf { it.isNotBlank() }
                ?: strength.takeIf { it.isNotBlank() }
                ?: brand,
            unit = strength.ifBlank { "علبة" },
            imageUrl = imageUrl,
            stockQuantity = stockQuantity,
            priceLabel = priceAmount?.let { amount ->
                val formatted = if (amount % 1.0 == 0.0) amount.toLong().toString() else "%.2f".format(amount)
                "$formatted $currency"
            },
        )
}
