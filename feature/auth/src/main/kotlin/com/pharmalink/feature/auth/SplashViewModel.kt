package com.pharmalink.feature.auth

import com.pharmalink.core.datastore.DataStoreManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val destinationRoute: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            runCatching {
                val role = prefs.accountType.first(); val user = authRepository.getCurrentUser().first()
                val route = when {user==null||role==null->"login_route";role=="PHARMACY"->"pharmacy_main_route";role=="WAREHOUSE"->"warehouse_main_route";else->"public_main_route"}
                _uiState.value = SplashUiState(isLoading = false, destinationRoute = route)
            }.onFailure { error ->
                Log.e("Auth", "Session check failed: ${error.message}", error)
                // ✅ في حال الخطأ، نوجه لـ Login كملاذ آمن
                _uiState.value = SplashUiState(
                    isLoading = false,
                    destinationRoute = "login_route",
                    errorMessage = error.message
                )
            }
        }
    }
}
