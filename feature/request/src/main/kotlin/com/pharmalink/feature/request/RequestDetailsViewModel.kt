package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Request
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RequestDetailsUiState(
    val screenState: ScreenState<Request> = ScreenState.Loading,
)

@HiltViewModel
class RequestDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
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
                _uiState.value = RequestDetailsUiState(
                    screenState = ScreenState.Error(context.getString(R.string.request_error_invalid_id)),
                )
                return@launch
            }
            
            try {
                _uiState.value = RequestDetailsUiState(ScreenState.Loading)
                
                pharmaRepository.getRequest(requestId).fold(
                    onSuccess = { request ->
                        _uiState.value = if (request != null) {
                            RequestDetailsUiState(ScreenState.Success(request))
                        } else {
                            RequestDetailsUiState(
                                screenState = ScreenState.Error(context.getString(R.string.request_error_not_found)),
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = RequestDetailsUiState(
                            screenState = ScreenState.Error(
                                e.message ?: context.getString(R.string.request_error_not_found),
                            ),
                        )
                    },
                )
            } catch (e: Exception) {
                _uiState.value = RequestDetailsUiState(
                    screenState = ScreenState.Error(mapErrorToUserMessage(e)),
                )
            }
        }
    }
    
    fun refreshRequest() {
        loadRequestDetails()
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
