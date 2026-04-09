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
        viewModelScope.launch {
            if (requestId.isBlank()) {
                _uiState.value = RequestDetailsUiState(
                    screenState = ScreenState.Error(context.getString(R.string.request_error_invalid_id)),
                )
                return@launch
            }
            val request = pharmaRepository.getRequest(requestId)
            _uiState.value = if (request != null) {
                RequestDetailsUiState(ScreenState.Success(request))
            } else {
                RequestDetailsUiState(
                    screenState = ScreenState.Error(context.getString(R.string.request_error_not_found)),
                )
            }
        }
    }
}
