package com.pharmalink.feature.admin.ui.dashboard

import androidx.compose.runtime.Immutable

@Immutable
data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: String = "",
    val adminName: String = "",
    
    // Basic Counts
    val totalUsers: Int = 0,
    val totalPharmacies: Int = 0,
    val totalWarehouses: Int = 0,
    val totalOrders: Int = 0,
    
    // Orders Breakdown
    val b2cOrdersCount: Int = 0,
    val b2bOrdersCount: Int = 0,
    val urgentOrdersCount: Int = 0,
    
    // Orders by Status
    val pendingOrdersCount: Int = 0,
    val confirmedOrdersCount: Int = 0,
    val deliveredOrdersCount: Int = 0,
    
    // Active Facilities
    val activePharmacies: Int = 0,
    val activeWarehouses: Int = 0,
    
    val pendingRequests: List<PendingRequestModel> = emptyList(),
    val recentActivities: List<ActivityModel> = emptyList(),
    val systemHealthPercent: Int = 0,
    val systemHealthStatus: String = "",
    // Note: activeConnections removed - telemetry not available in current schema
)

@Immutable
data class PendingRequestModel(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val timestamp: String = "",
    val type: RequestType = RequestType.FACILITY,
)

enum class RequestType {
    FACILITY,
    USER,
    ORDER,
}

@Immutable
data class ActivityModel(
    val id: String = "",
    val action: String = "",
    val user: String = "",
    val timestamp: String = "",
    val status: ActivityStatus = ActivityStatus.SUCCESS,
)

enum class ActivityStatus {
    SUCCESS,
    PENDING,
    FAILED,
}

sealed interface AdminDashboardAction {
    data object OnRetryClicked : AdminDashboardAction
    data object OnRefreshTriggered : AdminDashboardAction
    data object OnAddFacilityClicked : AdminDashboardAction
    data object OnGenerateReportClicked : AdminDashboardAction
    data object OnNotificationsClicked : AdminDashboardAction
    data object OnMenuClicked : AdminDashboardAction
    data object OnProfileClicked : AdminDashboardAction
    data object OnUsersCardClicked : AdminDashboardAction
    data object OnPharmaciesCardClicked : AdminDashboardAction
    data object OnWarehousesCardClicked : AdminDashboardAction
    data object OnOrdersCardClicked : AdminDashboardAction
    data class OnPendingRequestClicked(val requestId: String) : AdminDashboardAction
    data object OnViewAllRequestsClicked : AdminDashboardAction
    data object OnViewAllActivitiesClicked : AdminDashboardAction
}

sealed interface AdminDashboardEffect {
    data class ShowMessage(val message: String) : AdminDashboardEffect
    data object NavigateToAddFacility : AdminDashboardEffect
    data object NavigateToReports : AdminDashboardEffect
    data object NavigateToNotifications : AdminDashboardEffect
    data object NavigateToProfile : AdminDashboardEffect
    data object NavigateToUsers : AdminDashboardEffect
    data object NavigateToPharmacies : AdminDashboardEffect
    data object NavigateToWarehouses : AdminDashboardEffect
    data object NavigateToOrders : AdminDashboardEffect
    data class NavigateToOrderDetail(val orderId: String) : AdminDashboardEffect
    data object NavigateToAllActivities : AdminDashboardEffect
    data object ShowAdminMenu : AdminDashboardEffect
}
