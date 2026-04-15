package com.pharmalink.feature.home

import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Warehouse

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val stats: HomeStats?,
        val featuredWarehouses: List<Warehouse>,
        val alertMessage: String? = null,
        val isLoadingMore: Boolean = false,
        val error: String? = null
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

