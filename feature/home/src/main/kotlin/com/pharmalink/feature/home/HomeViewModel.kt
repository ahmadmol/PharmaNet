package com.pharmalink.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeDashboardContent(
    val activeOrders: Int,
    val urgentRequests: Int,
    val pendingOrders: Int,
    val completedToday: Int,
    /** Placeholder until inventory / low-stock signals exist in domain layer. */
    val lowStockAlerts: Int,
    val recentNotifications: List<AppNotification>,
    val recentRequests: List<Request>,
    val activeOrderItems: List<Order>,
)

data class HomeDashboardUiState(
    val greetingName: String = "PharmaLink",
    val notificationBadgeCount: Int = 0,
    val isRefreshing: Boolean = false,
    val screenState: ScreenState<HomeDashboardContent> = ScreenState.Loading,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                pharmaRepository.observeOrders(),
                pharmaRepository.observeRequests(),
                pharmaRepository.observeNotifications(),
            ) { orders, requests, notifications ->
                buildUiState(orders, requests, notifications)
            }.collect { nextState ->
                _uiState.update {
                    it.copy(
                        notificationBadgeCount = nextState.recentNotifications.count { notification -> !notification.read },
                        screenState = if (nextState.activeOrderItems.isEmpty() && nextState.recentRequests.isEmpty()) {
                            ScreenState.Empty
                        } else {
                            ScreenState.Success(nextState)
                        },
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun buildUiState(
        orders: List<Order>,
        requests: List<Request>,
        notifications: List<AppNotification>,
    ): HomeDashboardContent {
        val activeOrders = orders.filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.REJECTED }
        val urgentRequests = requests.filter { it.priority == RequestPriority.URGENT && it.status != RequestStatus.COMPLETED && it.status != RequestStatus.REJECTED }
        val completedToday = orders.count { it.status == OrderStatus.DELIVERED }

        return HomeDashboardContent(
            activeOrders = activeOrders.size,
            urgentRequests = urgentRequests.size,
            pendingOrders = orders.count { it.status == OrderStatus.PENDING },
            completedToday = completedToday,
            lowStockAlerts = 0,
            recentNotifications = notifications.take(3),
            recentRequests = requests.take(3),
            activeOrderItems = activeOrders.take(3),
        )
    }
}
