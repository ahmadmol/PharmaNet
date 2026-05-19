package com.pharmalink.feature.admin.ui.orders

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AdminOrder

@Immutable
data class AdminOrderDetailUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val order: AdminOrder? = null,
)

sealed interface AdminOrderDetailAction {
    data object OnRetryClicked : AdminOrderDetailAction
}

sealed interface AdminOrderDetailEffect {
    data class ShowMessage(val message: String) : AdminOrderDetailEffect
    data object NavigateBack : AdminOrderDetailEffect
}
