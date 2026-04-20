package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.RequestTransitions
import com.pharmalink.domain.model.RequestUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RequestDetailsUiState(
    val screenState: ScreenState<Request> = ScreenState.Loading,
    val accountType: AccountType? = null,
    val isActionInProgress: Boolean = false,
    val actionErrorMessage: String? = null,
)

@HiltViewModel
class RequestDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestDetailsUiState())
    val uiState: StateFlow<RequestDetailsUiState> = _uiState.asStateFlow()

    private val requestId: String = savedStateHandle.get<String>(NavArgs.REQUEST_ID).orEmpty()

    init {
        loadRequestDetails()
    }
    
    private fun loadRequestDetails() {
        viewModelScope.launch {
            if (requestId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Error(context.getString(R.string.request_error_invalid_id)),
                )
                return@launch
            }
            
            try {
                val userSnapshot = authRepository.getUserSnapshot()
                val userIdentity = userSnapshot?.toUserIdentity()
                val accountType = userIdentity?.role ?: userSnapshot?.accountType
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Loading,
                    accountType = accountType,
                )
                
                pharmaRepository.getRequest(requestId).fold(
                    onSuccess = { request ->
                        _uiState.value = if (request != null) {
                            _uiState.value.copy(screenState = ScreenState.Success(request))
                        } else {
                            _uiState.value.copy(
                                screenState = ScreenState.Error(context.getString(R.string.request_error_not_found)),
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            screenState = ScreenState.Error(
                                e.message ?: context.getString(R.string.request_error_not_found),
                            ),
                        )
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Error(mapErrorToUserMessage(e)),
                )
            }
        }
    }
    
    fun refreshRequest() {
        loadRequestDetails()
    }

    fun updateRequestStatus(targetStatus: RequestStatus) {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        if (_uiState.value.isActionInProgress) return
        val role = _uiState.value.accountType
        if (role != AccountType.WAREHOUSE) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }
        if (!RequestTransitions.canTransition(currentRequest.status, targetStatus, role)) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            pharmaRepository.updateRequest(
                requestId = currentRequest.id,
                updates = RequestUpdate(status = targetStatus),
            ).fold(
                onSuccess = { updatedRequest ->
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Success(updatedRequest),
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionErrorMessage = null)
    }
    
    private fun mapErrorToUserMessage(error: Throwable): String {
        return when {
            error.message?.contains("not found", ignoreCase = true) == true ||
            error.message?.contains("404", ignoreCase = true) == true ->
                context.getString(R.string.request_error_not_found)
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            error.message?.contains("deleted", ignoreCase = true) == true ->
                context.getString(R.string.request_error_deleted)
            else -> error.message ?: context.getString(R.string.request_error_loading_failed)
        }
    }
}
