package com.pharmalink.feature.admin.ui.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AdminUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: String = savedStateHandle[NavArgs.USER_ID] ?: ""

    private val _state = MutableStateFlow(UserDetailsUiState())
    val state: StateFlow<UserDetailsUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<UserDetailsEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<UserDetailsEffect> = _effect.asSharedFlow()

    init {
        if (userId.isEmpty()) {
            _state.update { it.copy(isLoading = false, contentError = "معرف المستخدم مفقود") }
        } else {
            loadUserDetails()
        }
    }

    fun onAction(action: UserDetailsAction) {
        when (action) {
            UserDetailsAction.OnRetryClicked -> loadUserDetails()
            UserDetailsAction.OnEditClicked -> {
                viewModelScope.launch {
                    _effect.emit(UserDetailsEffect.NavigateToEdit(userId))
                }
            }
            UserDetailsAction.OnDeactivateClicked -> {
                // No backend RPC available — button is disabled in UI
            }
            UserDetailsAction.OnResetPasswordClicked -> {
                // No backend RPC available — button is disabled in UI
            }
        }
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllUsers()
                .onSuccess { users ->
                    val user = users.find { it.id == userId }
                    if (user != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                user = user.toDetailModel(),
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                contentError = "المستخدم غير موجود",
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = USER_DETAILS_LOAD_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun AdminUser.toDetailModel(): UserDetailModel {
        val facilityName = when (accountType) {
            AccountType.PHARMACY -> pharmacyName ?: ""
            AccountType.WAREHOUSE -> warehouseName ?: ""
            else -> ""
        }

        // Note: Secondary stats (totalOrders, totalRequests, lastLoginDate)
        // are not included because endpoints are not available yet
        return UserDetailModel(
            id = id,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            accountType = accountType,
            facilityId = pharmacyId ?: warehouseId,
            facilityName = facilityName,
            isActive = isActive,
            createdAt = createdAt,
        )
    }
    private companion object {
        private const val USER_DETAILS_LOAD_ERROR_MESSAGE = "تعذر تحميل بيانات المستخدم. حاول مرة أخرى."
    }
}
