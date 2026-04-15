package com.pharmalink.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.UiState
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.UserSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthSessionState> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthSessionState.Loading,
        )

    val userSnapshot: StateFlow<UserSnapshot?> = authRepository.observeUserSnapshot()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState

    fun logout() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.logout()
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(Unit) },
                onFailure = { error -> UiState.Error(error.message ?: "Logout failed") },
            )
        }
    }
}
