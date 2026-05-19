package com.pharmalink.feature.auth.Splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AuthSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun routeUser(
        onNavigateToLogin: () -> Unit,
        onNavigateToAdminDashboard: () -> Unit,
        onNavigateToPharmacyHome: () -> Unit,
        onNavigateToWarehouseHome: () -> Unit,
        onNavigateToUserHome: () -> Unit,
    ) {
        viewModelScope.launch {
            val authState = authRepository.observeAuthState().first()

            if (authState !is AuthSessionState.Authenticated) {
                authRepository.clearUserSnapshot()
                onNavigateToLogin()
                return@launch
            }

            val user = authRepository.getUserSnapshot()
            when (user?.accountType) {
                AccountType.ADMIN -> onNavigateToAdminDashboard()
                AccountType.PHARMACY -> onNavigateToPharmacyHome()
                AccountType.WAREHOUSE -> onNavigateToWarehouseHome()
                AccountType.PUBLIC_USER -> onNavigateToUserHome()
                null -> {
                    authRepository.clearUserSnapshot()
                    onNavigateToLogin()
                }
            }
        }
    }
}
