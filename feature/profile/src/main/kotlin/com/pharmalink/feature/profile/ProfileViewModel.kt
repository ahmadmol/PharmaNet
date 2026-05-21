package com.pharmalink.feature.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.location.FacilityLocationService
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.feature.profile.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface ProfileUpdateStatus {
    data object Idle : ProfileUpdateStatus
    data object Loading : ProfileUpdateStatus
    data object Success : ProfileUpdateStatus
    data class Error(val message: String) : ProfileUpdateStatus
}

private const val TAG = "ProfileViewModel"
private const val PROFILE_UPDATE_SAFE_ERROR =
    "\u062a\u0639\u0630\u0631 \u062a\u062d\u062f\u064a\u062b \u0627\u0644\u0645\u0644\u0641 \u0627\u0644\u0634\u062e\u0635\u064a. \u064a\u0631\u062c\u0649 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 \u0644\u0627\u062d\u0642\u0627"
private const val WAREHOUSE_LOCATION_SAFE_ERROR =
    "\u062a\u0639\u0630\u0631 \u062a\u062d\u062f\u064a\u062b \u0645\u0648\u0642\u0639 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639. \u064a\u0631\u062c\u0649 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 \u0644\u0627\u062d\u0642\u0627"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: PharmaRepository,
    private val locationService: FacilityLocationService,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateStatus = MutableStateFlow<ProfileUpdateStatus>(ProfileUpdateStatus.Idle)
    val updateStatus: StateFlow<ProfileUpdateStatus> = _updateStatus.asStateFlow()

    private var currentProfile: PharmacyProfile? = null

    private fun buildSettingsOptions(): List<SettingItem> = listOf(
        SettingItem(
            context.getString(R.string.setting_item_notifications_title),
            context.getString(R.string.setting_item_notifications_subtitle)
        ),
        SettingItem(
            context.getString(R.string.setting_item_language_title),
            context.getString(R.string.setting_item_language_subtitle)
        ),
        SettingItem(
            context.getString(R.string.setting_item_about_title),
            context.getString(R.string.setting_item_about_subtitle)
        ),
        SettingItem(
            context.getString(R.string.setting_item_security_title),
            context.getString(R.string.setting_item_security_subtitle)
        ),
        SettingItem(
            context.getString(R.string.setting_item_help_title),
            context.getString(R.string.setting_item_help_subtitle)
        ),
        SettingItem(
            context.getString(R.string.setting_item_contact_title),
            context.getString(R.string.setting_item_contact_subtitle)
        )
    )

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeUserSnapshot(),
                pharmaRepository.observeProfile(),
            ) { snapshot, profile ->
                currentProfile = profile
                val userIdentity = snapshot?.toUserIdentity()
                val displayName = userIdentity?.displayName ?: snapshot?.displayName.orEmpty()
                val organizationNameFallback = when (snapshot?.accountType) {
                    AccountType.WAREHOUSE -> snapshot.warehouseName.ifBlank { snapshot.pharmacyName }
                    AccountType.PHARMACY -> snapshot.pharmacyName
                    else -> ""
                }
                val organizationName = userIdentity?.organizationName ?: organizationNameFallback
                ProfileUiState(
                    userName = profile.managerName.ifBlank {
                        displayName.ifBlank { context.getString(R.string.profile_default_user_name) }
                    },
                    userEmail = profile.contactEmail.ifBlank { snapshot?.email.orEmpty() },
                    userPhone = profile.contactPhone.ifBlank { snapshot?.phoneNumber.orEmpty() },
                    profileImageUrl = profile.avatarUrl,
                    accountType = if (snapshot?.accountType == AccountType.PUBLIC_USER) {
                        context.getString(R.string.profile_account_type_public_user)
                    } else {
                        snapshot?.accountType?.name?.replace('_', ' ').orEmpty()
                    },
                    accountTypeEnum = snapshot?.accountType,
                    pharmacyName = if (snapshot?.accountType == AccountType.PUBLIC_USER) {
                        ""
                    } else {
                        profile.pharmacyName.ifBlank { organizationName }
                    },
                    pharmacyAddress = profile.addressLine,
                    warehouseLatitude = profile.latitude,
                    warehouseLongitude = profile.longitude,
                    isUpdatingWarehouseLocation = _uiState.value.isUpdatingWarehouseLocation,
                    warehouseLocationMessage = _uiState.value.warehouseLocationMessage,
                    warehouseLocationMessageIsError = _uiState.value.warehouseLocationMessageIsError,
                    warehouseLocationSettingsAction = _uiState.value.warehouseLocationSettingsAction,
                    notificationsEnabled = profile.notificationsEnabled,
                    isUpdatingNotifications = _uiState.value.isUpdatingNotifications,
                    notificationsError = _uiState.value.notificationsError,
                    settingsOptions = buildSettingsOptions()
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
        avatarUri: Uri? = null,
    ) {
        val profile = currentProfile ?: return
        
        viewModelScope.launch {
            _updateStatus.value = ProfileUpdateStatus.Loading

            var uploadedAvatarUrl: String? = null
            val avatarUrl = if (avatarUri != null) {
                pharmaRepository.uploadProfileAvatar(avatarUri)
                    .onSuccess { uploadedAvatarUrl = it }
                    .getOrElse { error ->
                        Log.e(TAG, "Failed to upload profile avatar", error)
                        _updateStatus.value = ProfileUpdateStatus.Error(PROFILE_UPDATE_SAFE_ERROR)
                        return@launch
                    }
            } else {
                profile.avatarUrl
            }

            val updatedProfile = buildRoleAwareProfileUpdate(
                profile = profile,
                name = name,
                pharmacy = pharmacy,
                phone = phone,
                address = address,
                avatarUrl = avatarUrl,
            )
            
            pharmaRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _updateStatus.value = ProfileUpdateStatus.Success
                }
                .onFailure { e ->
                    uploadedAvatarUrl?.let { uploadedUrl ->
                        pharmaRepository.deleteProfileAvatar(uploadedUrl)
                    }
                    Log.e(TAG, "Failed to update profile", e)
                    _updateStatus.value = ProfileUpdateStatus.Error(PROFILE_UPDATE_SAFE_ERROR)
                }
        }
    }

    private fun buildRoleAwareProfileUpdate(
        profile: PharmacyProfile,
        name: String,
        pharmacy: String,
        phone: String,
        address: String,
        avatarUrl: String?,
    ): PharmacyProfile {
        val common = profile.copy(
            managerName = name,
            contactPhone = phone,
            contactEmail = _uiState.value.userEmail,
            notificationsEnabled = _uiState.value.notificationsEnabled,
            avatarUrl = avatarUrl,
        )

        return when (_uiState.value.accountTypeEnum) {
            AccountType.PUBLIC_USER -> common.copy(addressLine = address)
            AccountType.PHARMACY -> common.copy(
                pharmacyName = pharmacy,
                addressLine = address,
            )
            AccountType.ADMIN,
            AccountType.WAREHOUSE,
            null -> common
        }
    }

    fun updateWarehouseLocationFromCurrentGps() {
        if (_uiState.value.accountTypeEnum != AccountType.WAREHOUSE) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingWarehouseLocation = true,
                warehouseLocationMessage = null,
                warehouseLocationMessageIsError = false,
                warehouseLocationSettingsAction = null,
            )

            locationService.getCurrentFacilityLocation()
                .onSuccess { location ->
                    pharmaRepository.updateMyWarehouseLocation(
                        address = location.areaName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                    ).onSuccess {
                        _uiState.value = _uiState.value.copy(
                            pharmacyAddress = location.areaName,
                            warehouseLatitude = location.latitude,
                            warehouseLongitude = location.longitude,
                            isUpdatingWarehouseLocation = false,
                            warehouseLocationMessage = "تم تحديث موقع المستودع بنجاح",
                            warehouseLocationMessageIsError = false,
                            warehouseLocationSettingsAction = null,
                        )
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to update warehouse location", error)
                        _uiState.value = _uiState.value.copy(
                            isUpdatingWarehouseLocation = false,
                            warehouseLocationMessage = WAREHOUSE_LOCATION_SAFE_ERROR,
                            warehouseLocationMessageIsError = true,
                            warehouseLocationSettingsAction = null,
                        )
                    }
                }
                .onFailure { error ->
                    val action = when (error.message) {
                        "LOCATION_PERMISSION_DENIED" -> WarehouseLocationSettingsAction.APP_SETTINGS
                        "LOCATION_DISABLED" -> WarehouseLocationSettingsAction.LOCATION_SETTINGS
                        else -> null
                    }
                    val message = when (error.message) {
                        "LOCATION_PERMISSION_DENIED" -> "يرجى السماح بالوصول إلى الموقع من إعدادات التطبيق"
                        "LOCATION_DISABLED" -> "خدمات الموقع معطلة. يرجى تفعيل GPS من إعدادات الجهاز"
                        "LOCATION_UNAVAILABLE" -> "تعذر تحديد الموقع الحالي. حاول مرة أخرى"
                        else -> error.message ?: "تعذر تحديث موقع المستودع"
                    }
                    _uiState.value = _uiState.value.copy(
                        isUpdatingWarehouseLocation = false,
                        warehouseLocationMessage = message,
                        warehouseLocationMessageIsError = true,
                        warehouseLocationSettingsAction = action,
                    )
                }
        }
    }

    fun onWarehouseLocationPermissionDenied(permanentlyDenied: Boolean) {
        if (_uiState.value.accountTypeEnum != AccountType.WAREHOUSE) return

        _uiState.value = _uiState.value.copy(
            isUpdatingWarehouseLocation = false,
            warehouseLocationMessage = if (permanentlyDenied) {
                "يرجى السماح بالوصول إلى الموقع من إعدادات التطبيق"
            } else {
                "يرجى السماح بالوصول إلى الموقع لتحديث موقع المستودع"
            },
            warehouseLocationMessageIsError = true,
            warehouseLocationSettingsAction = if (permanentlyDenied) {
                WarehouseLocationSettingsAction.APP_SETTINGS
            } else {
                null
            },
        )
    }

    fun resetUpdateStatus() {
        _updateStatus.value = ProfileUpdateStatus.Idle
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingNotifications = true,
                notificationsError = null,
            )
            val result = pharmaRepository.updateNotificationsPreference(enabled)
            _uiState.value = _uiState.value.copy(
                isUpdatingNotifications = false,
                notificationsEnabled = if (result.isSuccess) enabled else _uiState.value.notificationsEnabled,
                notificationsError = result.exceptionOrNull()?.message,
            )
        }
    }
}
