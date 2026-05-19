package com.pharmalink.feature.auth.Splash

import com.pharmalink.domain.model.AccountType

sealed class SplashScreenUiState {
    object Loading : SplashScreenUiState()
    data class Navigate(val accountType: AccountType?) : SplashScreenUiState()
}