package com.pharmalink.feature.home

import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import com.pharmalink.domain.model.Warehouse

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val stats: HomeStats?,
        val featuredWarehouses: List<Warehouse>,
        val accountType: AccountType? = null,
        val canAddMedicine: Boolean = false,
        val alertMessage: String? = null,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        // Public-user discovery data (B2C home). Empty for non-public accounts.
        val publicPharmacies: List<PublicPharmacyForMedicine> = emptyList(),
        val featuredMedicines: List<Medicine> = emptyList(),
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
