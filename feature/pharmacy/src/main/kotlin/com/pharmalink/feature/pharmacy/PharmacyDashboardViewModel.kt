package com.pharmalink.feature.pharmacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PharmacyDashboardViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyDashboardUiState())
    val uiState: StateFlow<PharmacyDashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            // Determine pharmacy linkage from user snapshot
            val snapshot = authRepository.getUserSnapshot()
            val isPharmacy = snapshot?.accountType == AccountType.PHARMACY
            val orgId = snapshot?.toUserIdentity()?.organizationId?.takeIf { it.isNotBlank() }
            val isLinked = isPharmacy && orgId != null

            _uiState.update { it.copy(isLoading = true, errorMessage = null, isPharmacyLinked = isLinked) }

            val statsDeferred = async { pharmaRepository.fetchHomeStats() }
            val ordersDeferred = async { pharmaRepository.getPharmacyCustomerOrders() }

            val statsResult = statsDeferred.await()
            val ordersResult = ordersDeferred.await()

            // The customer-orders inbox is the primary pharmacy signal. If it fails we treat the
            // dashboard stats as unavailable; navigation cards stay visible regardless.
            if (ordersResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ordersResult.exceptionOrNull()?.message
                            ?: "تعذر تحميل ملخص النشاط",
                    )
                }
                return@launch
            }

            val orders = ordersResult.getOrNull().orEmpty()
            val pendingCount = orders.count { it.status == OrderStatus.PENDING }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    requestsTodayCount = statsResult.getOrNull()?.requestsTodayCount,
                    pendingCustomerOrdersCount = pendingCount,
                    totalCustomerOrdersCount = orders.size,
                    errorMessage = null,
                )
            }
        }
    }
}
