package com.pharmalink.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUpdateStatus {
    data object Idle : ProfileUpdateStatus
    data object Loading : ProfileUpdateStatus
    data object Success : ProfileUpdateStatus
    data class Error(val message: String) : ProfileUpdateStatus
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateStatus = MutableStateFlow<ProfileUpdateStatus>(ProfileUpdateStatus.Idle)
    val updateStatus: StateFlow<ProfileUpdateStatus> = _updateStatus.asStateFlow()

    private var currentProfileId: String? = null

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeUserSnapshot(),
                pharmaRepository.observeProfile(),
            ) { snapshot, profile ->
                currentProfileId = profile.id
                val userIdentity = snapshot?.toUserIdentity()
                val displayName = userIdentity?.displayName ?: snapshot?.displayName.orEmpty()
                val organizationNameFallback = when (snapshot?.accountType) {
                    AccountType.WAREHOUSE -> snapshot.warehouseName.ifBlank { snapshot.pharmacyName }
                    AccountType.PHARMACY -> snapshot.pharmacyName
                    else -> ""
                }
                val organizationName = userIdentity?.organizationName ?: organizationNameFallback
                ProfileUiState(
                    userName = profile.managerName.ifBlank { displayName.ifBlank { "مستخدم" } },
                    userEmail = profile.contactEmail.ifBlank { snapshot?.email.orEmpty() },
                    userPhone = profile.contactPhone.ifBlank { snapshot?.phoneNumber.orEmpty() },
                    accountType = snapshot?.accountType?.name?.replace('_', ' ').orEmpty(),
                    pharmacyName = profile.pharmacyName.ifBlank { organizationName },
                    pharmacyAddress = profile.addressLine,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateProfile(
        name: String,
        pharmacy: String,
        phone: String,
        address: String,
    ) {
        val profileId = currentProfileId ?: return
        
        viewModelScope.launch {
            _updateStatus.value = ProfileUpdateStatus.Loading
            
            val updatedProfile = com.pharmalink.domain.model.PharmacyProfile(
                id = profileId,
                managerName = name,
                pharmacyName = pharmacy,
                contactPhone = phone,
                addressLine = address,
                contactEmail = _uiState.value.userEmail,
                city = "",
                district = "",
                licenseStatusLabel = "",
                licenseNumber = "",
                licenseExpiryLabel = "",
                operatingHoursLabel = "",
                preferredLanguageLabel = "",
                notificationsEnabled = true,
                twoFactorEnabled = false,
                linkedDevicesCount = 0,
                totalOrders = 0,
                completedOrders = 0,
                activeOrders = 0,
            )
            
            pharmaRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _updateStatus.value = ProfileUpdateStatus.Success
                }
                .onFailure { e ->
                    _updateStatus.value = ProfileUpdateStatus.Error(e.message ?: "فشل تحديث الملف الشخصي")
                }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = ProfileUpdateStatus.Idle
    }
}
