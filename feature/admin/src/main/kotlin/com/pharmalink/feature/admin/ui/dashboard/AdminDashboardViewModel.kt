package com.pharmalink.feature.admin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
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
                    _effect.emit(AdminDashboardEffect.NavigateToReports)
                }
            }
            AdminDashboardAction.OnNotificationsClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToNotifications)
                }
            }
            AdminDashboardAction.OnMenuClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.ShowAdminMenu)
                }
            }
            AdminDashboardAction.OnProfileClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminDashboardEffect.NavigateToProfile)
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
                    _effect.emit(AdminDashboardEffect.NavigateToOrders)
                }
            }
            is AdminDashboardAction.OnPendingRequestClicked -> {
                viewModelScope.launch {
                    if (action.requestType == RequestType.ORDER) {
                        _effect.emit(AdminDashboardEffect.NavigateToOrderDetail(action.requestId))
                    } else {
                        _effect.emit(AdminDashboardEffect.ShowMessage("تفاصيل هذا الطلب غير متاحة من لوحة التحكم حاليا"))
                    }
                }
            }
            AdminDashboardAction.OnViewAllRequestsClicked -> {
                viewModelScope.launch {
                    // Navigate to orders screen with pending filter
                    _effect.emit(AdminDashboardEffect.NavigateToOrders)
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
                    // Load additional data in parallel
                    val pendingRequestsDeferred = async { pharmaRepository.adminGetPendingRequests(5) }
                    val recentActivitiesDeferred = async { pharmaRepository.adminGetRecentActivities(5) }
                    val systemHealthDeferred = async { pharmaRepository.adminGetSystemHealth() }

                    val pendingRequestsResult = pendingRequestsDeferred.await()
                    val recentActivitiesResult = recentActivitiesDeferred.await()
                    val systemHealthResult = systemHealthDeferred.await()

                    // Map domain models to UI models
                    val pendingRequests = pendingRequestsResult.getOrNull()?.map { req ->
                        PendingRequestModel(
                            id = req.id,
                            title = req.title,
                            subtitle = req.subtitle,
                            timestamp = req.timestamp,
                            type = when (req.requestType) {
                                "ORDER" -> RequestType.ORDER
                                "FACILITY" -> RequestType.FACILITY
                                "USER" -> RequestType.USER
                                else -> RequestType.UNKNOWN
                            },
                        )
                    } ?: emptyList()

                    val recentActivities = recentActivitiesResult.getOrNull()?.map { activity ->
                        ActivityModel(
                            id = activity.id,
                            action = activity.action,
                            user = activity.userName,
                            timestamp = activity.timestamp,
                            status = when (activity.status) {
                                "SUCCESS" -> ActivityStatus.SUCCESS
                                "FAILED" -> ActivityStatus.FAILED
                                "PENDING" -> ActivityStatus.PENDING
                                else -> ActivityStatus.SUCCESS
                            },
                        )
                    } ?: emptyList()

                    val systemHealth = systemHealthResult.getOrNull()

                    _state.update {
                        it.copy(
                            isLoading = false,
                            adminName = "",  // resolved in Screen via stringResource
                            totalUsers = stats.totalUsers,
                            totalPharmacies = stats.totalPharmacies,
                            totalWarehouses = stats.totalWarehouses,
                            totalOrders = stats.totalOrders,
                            b2cOrdersCount = stats.b2cOrdersCount,
                            b2bOrdersCount = stats.b2bOrdersCount,
                            urgentOrdersCount = stats.urgentOrdersCount,
                            pendingOrdersCount = stats.pendingOrdersCount,
                            confirmedOrdersCount = stats.confirmedOrdersCount,
                            deliveredOrdersCount = stats.deliveredOrdersCount,
                            activePharmacies = stats.activePharmacies,
                            activeWarehouses = stats.activeWarehouses,
                            pendingRequests = pendingRequests,
                            recentActivities = recentActivities,
                            systemHealthPercent = systemHealth?.healthPercent ?: 0,
                            systemHealthStatus = systemHealth?.healthStatus ?: "--",
                            // Note: activeConnections removed - not available in current schema
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message.orEmpty(),
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
                    // Load additional data in parallel
                    val pendingRequestsDeferred = async { pharmaRepository.adminGetPendingRequests(5) }
                    val recentActivitiesDeferred = async { pharmaRepository.adminGetRecentActivities(5) }
                    val systemHealthDeferred = async { pharmaRepository.adminGetSystemHealth() }

                    val pendingRequestsResult = pendingRequestsDeferred.await()
                    val recentActivitiesResult = recentActivitiesDeferred.await()
                    val systemHealthResult = systemHealthDeferred.await()

                    // Map domain models to UI models
                    val pendingRequests = pendingRequestsResult.getOrNull()?.map { req ->
                        PendingRequestModel(
                            id = req.id,
                            title = req.title,
                            subtitle = req.subtitle,
                            timestamp = req.timestamp,
                            type = when (req.requestType) {
                                "ORDER" -> RequestType.ORDER
                                "FACILITY" -> RequestType.FACILITY
                                "USER" -> RequestType.USER
                                else -> RequestType.UNKNOWN
                            },
                        )
                    } ?: emptyList()

                    val recentActivities = recentActivitiesResult.getOrNull()?.map { activity ->
                        ActivityModel(
                            id = activity.id,
                            action = activity.action,
                            user = activity.userName,
                            timestamp = activity.timestamp,
                            status = when (activity.status) {
                                "SUCCESS" -> ActivityStatus.SUCCESS
                                "FAILED" -> ActivityStatus.FAILED
                                "PENDING" -> ActivityStatus.PENDING
                                else -> ActivityStatus.SUCCESS
                            },
                        )
                    } ?: emptyList()

                    val systemHealth = systemHealthResult.getOrNull()

                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            totalUsers = stats.totalUsers,
                            totalPharmacies = stats.totalPharmacies,
                            totalWarehouses = stats.totalWarehouses,
                            totalOrders = stats.totalOrders,
                            b2cOrdersCount = stats.b2cOrdersCount,
                            b2bOrdersCount = stats.b2bOrdersCount,
                            urgentOrdersCount = stats.urgentOrdersCount,
                            pendingOrdersCount = stats.pendingOrdersCount,
                            confirmedOrdersCount = stats.confirmedOrdersCount,
                            deliveredOrdersCount = stats.deliveredOrdersCount,
                            activePharmacies = stats.activePharmacies,
                            activeWarehouses = stats.activeWarehouses,
                            pendingRequests = pendingRequests,
                            recentActivities = recentActivities,
                            systemHealthPercent = systemHealth?.healthPercent ?: 0,
                            systemHealthStatus = systemHealth?.healthStatus ?: "--",
                            // Note: activeConnections removed - not available in current schema
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(
                        AdminDashboardEffect.ShowMessage("فشل تحديث لوحة التحكم"),
                    )
                }
        }
    }

}
