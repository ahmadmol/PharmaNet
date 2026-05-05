package com.pharmalink.feature.admin.ui.pharmacies

import androidx.compose.runtime.Immutable

@Immutable
data class PharmacyDetailsUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val pharmacy: PharmacyDetailModel? = null,
)

@Immutable
data class PharmacyDetailModel(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val contactNumber: String = "",
    val licenseNumber: String = "",
    val isActive: Boolean = false,
    val createdAt: String = "",
    val totalEmployees: Int = 0,
    val totalOrders: Int = 0,
    val totalCustomers: Int = 0,
    val averageRating: Float = 0f,
)

sealed interface PharmacyDetailsAction {
    data object OnRetryClicked : PharmacyDetailsAction
    data object OnManageBranchClicked : PharmacyDetailsAction
    data object OnViewOrdersClicked : PharmacyDetailsAction
    data object OnEditClicked : PharmacyDetailsAction
}

sealed interface PharmacyDetailsEffect {
    data class ShowMessage(val message: String) : PharmacyDetailsEffect
    data class NavigateToBranchManagement(val pharmacyId: String) : PharmacyDetailsEffect
    data class NavigateToOrders(val pharmacyId: String) : PharmacyDetailsEffect
}
