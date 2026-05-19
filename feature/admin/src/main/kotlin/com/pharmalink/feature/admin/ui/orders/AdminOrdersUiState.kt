package com.pharmalink.feature.admin.ui.orders

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AdminOrder

@Immutable
data class AdminOrdersUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val orders: List<AdminOrder> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: OrderFilter = OrderFilter.ALL,
    val hasMore: Boolean = false,
)

enum class OrderFilter {
    ALL,
    B2C,
    B2B,
    URGENT,
    PENDING,
}

sealed interface AdminOrdersAction {
    data object OnRetryClicked : AdminOrdersAction
    data object OnRefreshTriggered : AdminOrdersAction
    data class OnSearchQueryChanged(val query: String) : AdminOrdersAction
    data class OnFilterSelected(val filter: OrderFilter) : AdminOrdersAction
    data class OnOrderClicked(val orderId: String) : AdminOrdersAction
    data object OnLoadMore : AdminOrdersAction
}

sealed interface AdminOrdersEffect {
    data class ShowMessage(val message: String) : AdminOrdersEffect
    data class NavigateToOrderDetail(val orderId: String) : AdminOrdersEffect
}
