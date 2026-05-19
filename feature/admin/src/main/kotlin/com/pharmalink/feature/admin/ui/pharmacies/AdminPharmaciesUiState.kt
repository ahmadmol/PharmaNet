package com.pharmalink.feature.admin.ui.pharmacies

import androidx.compose.runtime.Immutable

@Immutable
data class AdminPharmaciesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: String = "",
    val pharmacies: List<PharmacyItemModel> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: PharmacyFilterStatus = PharmacyFilterStatus.ALL,
    val sortBy: PharmacySortBy = PharmacySortBy.NAME,
)

enum class PharmacyFilterStatus {
    ALL,
    ACTIVE,
    INACTIVE
}

enum class PharmacySortBy {
    NAME,
    LOCATION,
    DATE_ADDED
}

@Immutable
data class PharmacyItemModel(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val isActive: Boolean = false,
    val createdAt: String = "",
)

sealed interface AdminPharmaciesAction {
    data object OnRetryClicked : AdminPharmaciesAction
    data object OnRefreshTriggered : AdminPharmaciesAction
    data object OnMenuClicked : AdminPharmaciesAction
    data class OnSearchQueryChanged(val query: String) : AdminPharmaciesAction
    data object OnFilterClicked : AdminPharmaciesAction
    data class OnFilterStatusChanged(val status: PharmacyFilterStatus) : AdminPharmaciesAction
    data class OnSortByChanged(val sortBy: PharmacySortBy) : AdminPharmaciesAction
    data class OnPharmacyClicked(val pharmacyId: String) : AdminPharmaciesAction
    data class OnManageBranchClicked(val pharmacyId: String) : AdminPharmaciesAction
    data object OnAddPharmacyClicked : AdminPharmaciesAction
    data object OnCoverageMapClicked : AdminPharmaciesAction
}

sealed interface AdminPharmaciesEffect {
    data class ShowMessage(val message: String) : AdminPharmaciesEffect
    data object ShowAdminMenu : AdminPharmaciesEffect
    data class NavigateToPharmacyDetail(val pharmacyId: String) : AdminPharmaciesEffect
    data class NavigateToBranchManagement(val pharmacyId: String) : AdminPharmaciesEffect
    data object NavigateToCoverageMap : AdminPharmaciesEffect
}
