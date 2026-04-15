package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
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
    val selectedStatus: RequestStatus? = null
)

@HiltViewModel
class RequestListViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
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

            pharmaRepository.observeRequests()
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
        return when {
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            else -> error.message ?: context.getString(R.string.request_error_loading_failed)
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
