package com.pharmalink.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.User
import com.pharmalink.domain.model.UserSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun checkSession() {
        if (observeJob != null) return

        observeJob = viewModelScope.launch {
            _uiState.value = SplashUiState(isLoading = true)
            when (
                val authState = authRepository.observeAuthState()
                    .first { state -> state !is AuthSessionState.Loading }
            ) {
                is AuthSessionState.Authenticated -> restoreSnapshotAndProceed(authState.user)
                AuthSessionState.Unauthenticated -> {
                    _uiState.value = SplashUiState(
                        isLoading = false,
                    )
                }
                AuthSessionState.Loading -> Unit
            }
        }
    }

    private suspend fun restoreSnapshotAndProceed(user: User) {
        val existingSnapshot = authRepository.getUserSnapshot()
        
        if (existingSnapshot == null) {
            runCatching {
                authRepository.ensureProfileForCurrentUser(user).getOrThrow()
                _uiState.value = SplashUiState(isLoading = false)
            }.onFailure { error ->
                _uiState.value = SplashUiState(
                    isLoading = false,
                    errorMessage = error.message,
                )
            }
        } else {
            _uiState.value = SplashUiState(isLoading = false)
            viewModelScope.launch {
                verifyIdentityInBackground(user, existingSnapshot)
            }
        }
    }

    private suspend fun verifyIdentityInBackground(user: User, cachedSnapshot: UserSnapshot) {
        authRepository.bootstrapAuthenticatedUser(user)
            .onSuccess { newSnapshot ->
                if (newSnapshot.pharmacyId != cachedSnapshot.pharmacyId) {
                    // Silent update already handled by saveUserSnapshot in bootstrap
                }
            }
            .onFailure { error ->
                if (error is MissingPharmacyLinkageException) {
                    authRepository.clearUserSnapshot()
                    _uiState.value = SplashUiState(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
    }
}
