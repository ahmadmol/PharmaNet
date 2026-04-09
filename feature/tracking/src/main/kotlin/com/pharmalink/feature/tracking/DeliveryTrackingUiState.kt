package com.pharmalink.feature.tracking

import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.domain.model.DeliveryTracking

data class DeliveryTrackingUiState(
    val screenState: ScreenState<DeliveryTracking> = ScreenState.Loading,
    val isRefreshing: Boolean = false,
    val canCallDelegate: Boolean = true,
)
