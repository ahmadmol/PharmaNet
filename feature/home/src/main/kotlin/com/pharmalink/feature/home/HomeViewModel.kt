package com.pharmalink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.OrganizationType
import kotlinx.coroutines.async

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val userSnapshot = authRepository.getUserSnapshot()
            val userIdentity = userSnapshot?.toUserIdentity()
            val identityMatches = userIdentity?.role == userSnapshot?.accountType
            val isWarehouseFromIdentity =
                userIdentity?.organizationType == OrganizationType.WAREHOUSE
            val currentUserName = userSnapshot?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"
            val accountType = userSnapshot?.accountType
            val canAddMedicine = if (userIdentity != null) {
                isWarehouseFromIdentity
            } else {
                accountType == AccountType.WAREHOUSE
            }
            
            _uiState.value = HomeUiState.Loading

            if (accountType == AccountType.PUBLIC_USER) {
                // Load lightweight discovery data so the public home is not empty.
                // Both calls are best-effort: a failure degrades to an empty section
                // rather than blocking the whole screen.
                val pharmaciesDeferred = async { pharmaRepository.getPublicPharmacies() }
                val medicinesDeferred = async { pharmaRepository.fetchMedicines() }

                val publicPharmacies = pharmaciesDeferred.await().getOrDefault(emptyList()).take(5)
                val featuredMedicines = medicinesDeferred.await().getOrDefault(emptyList()).take(5)

                _uiState.value = HomeUiState.Success(
                    userName = currentUserName,
                    stats = null,
                    featuredWarehouses = emptyList(),
                    accountType = accountType,
                    canAddMedicine = false,
                    alertMessage = null,
                    publicPharmacies = publicPharmacies,
                    featuredMedicines = featuredMedicines,
                )
                return@launch
            }
            
            val statsDeferred = async { pharmaRepository.fetchHomeStats() }
            val warehousesDeferred = async { pharmaRepository.fetchFeaturedWarehouses() }
            
            val statsResult = statsDeferred.await()
            val warehousesResult = warehousesDeferred.await()
            
            if (statsResult.isFailure || warehousesResult.isFailure) {
                val error = statsResult.exceptionOrNull() ?: warehousesResult.exceptionOrNull()
                _uiState.value = HomeUiState.Error(error?.message ?: "فشل تحميل بيانات الصفحة الرئيسية")
            } else {
                val stats = statsResult.getOrThrow()
                val warehouses = warehousesResult.getOrThrow()
                _uiState.value = HomeUiState.Success(
                    userName = currentUserName,
                    stats = stats,
                    featuredWarehouses = warehouses,
                    accountType = accountType,
                    canAddMedicine = canAddMedicine,
                    alertMessage = stats.alertMessage
                )
            }
        }
    }
}
