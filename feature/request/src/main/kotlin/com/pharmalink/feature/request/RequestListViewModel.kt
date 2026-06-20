package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestListUiState(
    val requests: List<Request> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedStatus: RequestStatus? = null,
    val accountType: AccountType? = null,
)

@HiltViewModel
class RequestListViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestListUiState())
    val uiState: StateFlow<RequestListUiState> = _uiState.asStateFlow()
    private var requestsJob: Job? = null
    private var latestRequests: List<Request> = emptyList()

    init {
        loadRequests()
    }

    fun loadRequests() {
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Get user identity for role-based flow selection
            val userSnapshot = authRepository.getUserSnapshot()
            val userIdentity = userSnapshot?.toUserIdentity()
            val accountType = userIdentity?.role ?: userSnapshot?.accountType
            _uiState.value = _uiState.value.copy(accountType = accountType)

            // Select appropriate data flow based on account type.
            // observeRequests() is role-aware and handles filtering internally.
            val requestsFlow = pharmaRepository.observeRequests()

            requestsFlow
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToUserMessage(error),
                    )
                }
                .collect { requests ->
                    latestRequests = requests
                    applyFilteredRequests(
                        requests = requests,
                        selectedStatus = _uiState.value.selectedStatus,
                    )
                }
        }
    }

    fun filterByStatus(status: RequestStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        applyFilteredRequests(
            requests = latestRequests,
            selectedStatus = status,
        )
    }
    
    private fun filterRequestsByStatus(requests: List<Request>, status: RequestStatus?): List<Request> {
        return if (status != null) {
            requests.filter { it.status == status }
        } else {
            requests
        }
    }
    
    private fun mapErrorToUserMessage(error: Throwable): String {
        val message = error.message ?: ""
        return when {
            message.contains("network", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ->
                context.getString(R.string.error_network)
            message.contains("permission", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.error_permission)
            message.contains("missing organizationId", ignoreCase = true) ->
                "حسابك غير مرتبط بصيدلية أو مستودع حالياً. يرجى مراجعة الإدارة لإتمام الربط."
            else -> context.getString(R.string.request_error_loading_failed)
        }
    }

    private fun applyFilteredRequests(
        requests: List<Request>,
        selectedStatus: RequestStatus?,
    ) {
        val filteredRequests = filterRequestsByStatus(requests, selectedStatus)
        val currentRequests = _uiState.value.requests
        if (currentRequests != filteredRequests) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                requests = filteredRequests,
                errorMessage = null,
            )
        } else if (_uiState.value.isLoading) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshRequests() {
        loadRequests()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
