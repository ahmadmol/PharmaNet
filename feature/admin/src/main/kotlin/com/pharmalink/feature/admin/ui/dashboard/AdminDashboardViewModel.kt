package com.pharmalink.feature.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state: StateFlow<AdminDashboardUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminDashboardEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminDashboardEffect> = _effect.asSharedFlow()

    init {
        loadDashboard()
    }

    fun onAction(action: AdminDashboardAction) {
        when (action) {
            AdminDashboardAction.OnRetryClicked -> loadDashboard()
            AdminDashboardAction.OnRefreshTriggered -> refreshDashboard()
            AdminDashboardAction.OnAddFacilityClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToAddFacility)
                }
            }
            AdminDashboardAction.OnGenerateReportClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowMessage("إنشاء التقرير: قيد التطوير"))
                }
            }
            AdminDashboardAction.OnNotificationsClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToNotifications)
                }
            }
            AdminDashboardAction.OnMenuClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowMessage("القائمة: قيد التطوير"))
                }
            }
            AdminDashboardAction.OnUsersCardClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToUsers)
                }
            }
            AdminDashboardAction.OnPharmaciesCardClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToPharmacies)
                }
            }
            AdminDashboardAction.OnWarehousesCardClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToWarehouses)
                }
            }
            AdminDashboardAction.OnOrdersCardClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowMessage("الطلبات: قيد التطوير"))
                }
            }
            is AdminDashboardAction.OnPendingRequestClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowMessage("تفاصيل الطلب: قيد التطوير"))
                }
            }
            AdminDashboardAction.OnViewAllRequestsClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowMessage("جميع الطلبات: قيد التطوير"))
                }
            }
            AdminDashboardAction.OnViewAllActivitiesClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToAllActivities)
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetDashboardStats()
                .onSuccess { stats ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            adminName = "مدير النظام",
                            totalUsers = stats.totalUsers,
                            totalPharmacies = stats.totalPharmacies,
                            totalWarehouses = stats.totalWarehouses,
                            totalOrders = stats.totalOrders,
                            pendingRequests = emptyList(), // TODO: fetch real pending requests when endpoint is added
                            recentActivities = emptyList(), // TODO: fetch real activities when endpoint is added
                            systemHealthPercent = 0, // TODO: fetch real system health when endpoint is added
                            systemHealthStatus = "--",
                            activeConnections = 0, // TODO: fetch real connections when endpoint is added
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message ?: "فشل تحميل بيانات لوحة التحكم",
                        )
                    }
                }
        }
    }

    private fun refreshDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            pharmaRepository.adminGetDashboardStats()
                .onSuccess { stats ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            totalUsers = stats.totalUsers,
                            totalPharmacies = stats.totalPharmacies,
                            totalWarehouses = stats.totalWarehouses,
                            totalOrders = stats.totalOrders,
                            pendingRequests = emptyList(), // TODO: fetch real pending requests when endpoint is added
                            recentActivities = emptyList(), // TODO: fetch real activities when endpoint is added
                            systemHealthPercent = 0, // TODO: fetch real system health when endpoint is added
                            activeConnections = 0, // TODO: fetch real connections when endpoint is added
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(AdminDashboardEffect.ShowMessage("فشل تحديث البيانات"))
                }
        }
    }

}
