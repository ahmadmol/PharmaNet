package com.pharmalink.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.PharmacyCustomerOrder
import com.pharmalink.feature.orders.usecase.GetPharmacyCustomerOrdersUseCase
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

data class PharmacyCustomerOrdersUiState(
    val screenState: ScreenState<List<PharmacyCustomerOrderUi>> = ScreenState.Loading,
    val selectedFilter: PharmacyCustomerOrderFilter = PharmacyCustomerOrderFilter.ALL,
    val pendingCount: Int = 0,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class PharmacyCustomerOrdersViewModel @Inject constructor(
    private val getOrders: GetPharmacyCustomerOrdersUseCase,
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyCustomerOrdersUiState())
    val uiState: StateFlow<PharmacyCustomerOrdersUiState> = _uiState.asStateFlow()

    private val _newOrderNotification = MutableSharedFlow<String>()
    val newOrderNotification: SharedFlow<String> = _newOrderNotification.asSharedFlow()

    private var allOrders: List<PharmacyCustomerOrder> = emptyList()

    init {
        loadOrders()
        observeNotifications()
    }

    fun refreshOrders() {
        loadOrders(refreshing = true)
    }

    fun selectFilter(filter: PharmacyCustomerOrderFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        publishFilteredOrders()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            repository.observeNotifications().collect { notifications ->
                val latestNotification = notifications.firstOrNull() ?: return@collect
                
                // Check if it's a new B2C order notification
                if (latestNotification.destination == NotificationDestination.PHARMACY_CUSTOMER_ORDER && !latestNotification.read) {
                    _newOrderNotification.emit("وصل طلب جديد، تم تحديث القائمة")
                    loadOrders(refreshing = true)
                }
            }
        }
    }

    private fun loadOrders(refreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screenState = if (refreshing && allOrders.isNotEmpty()) it.screenState else ScreenState.Loading,
                    isRefreshing = refreshing,
                )
            }

            getOrders().fold(
                onSuccess = { orders ->
                    allOrders = orders
                    publishFilteredOrders(isRefreshing = false)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Error(error.message ?: "تعذر تحميل طلبات العملاء"),
                            isRefreshing = false,
                        )
                    }
                },
            )
        }
    }

    private fun publishFilteredOrders(isRefreshing: Boolean = _uiState.value.isRefreshing) {
        val filter = _uiState.value.selectedFilter
        val filtered = allOrders
            .filter { it.matches(filter) }
            .map { it.toPharmacyCustomerOrderUi() }

        _uiState.update {
            it.copy(
                screenState = if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Success(filtered),
                pendingCount = allOrders.count { order -> order.status == com.pharmalink.domain.model.OrderStatus.PENDING },
                isRefreshing = isRefreshing,
            )
        }
    }
}
