package com.pharmalink.feature.pharmacy

import androidx.compose.runtime.Immutable

@Immutable
data class WarehouseProductsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val products: List<WarehouseProductUiModel> = emptyList(),
)

@Immutable
data class WarehouseProductUiModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val unit: String,
    val imageUrl: String?,
    val stockQuantity: Int,
    val priceLabel: String?,
)
