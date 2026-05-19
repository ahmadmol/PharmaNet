package com.pharmalink.feature.profile

import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.UserSnapshot

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val accountType: String = "",
    val accountTypeEnum: AccountType? = null,
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val profileImageUrl: String? = null,
    val notificationsEnabled: Boolean = true,
    val isUpdatingNotifications: Boolean = false,
    val notificationsError: String? = null,
    val warehouseLatitude: Double? = null,
    val warehouseLongitude: Double? = null,
    val isUpdatingWarehouseLocation: Boolean = false,
    val warehouseLocationMessage: String? = null,
    val warehouseLocationMessageIsError: Boolean = false,
    val warehouseLocationSettingsAction: WarehouseLocationSettingsAction? = null,
    val settingsOptions: List<SettingItem> = emptyList(),
) {
    companion object {
        fun fromSnapshot(snapshot: UserSnapshot?): ProfileUiState =
            ProfileUiState(
                userName = snapshot?.displayName.orEmpty().ifBlank { "مستخدم" },
                userEmail = snapshot?.email.orEmpty(),
                userPhone = snapshot?.phoneNumber.orEmpty(),
                accountType = snapshot?.accountType?.name?.replace('_', ' ') ?: "",
                accountTypeEnum = snapshot?.accountType,
                pharmacyName = when (snapshot?.accountType) {
                    AccountType.WAREHOUSE -> snapshot.warehouseName.ifBlank { snapshot.pharmacyName }
                    else -> snapshot?.pharmacyName.orEmpty()
                },
                pharmacyAddress = when (snapshot?.accountType) {
                    AccountType.WAREHOUSE -> snapshot.warehouseId.ifBlank { snapshot.pharmacyId }
                    else -> snapshot?.pharmacyId.orEmpty()
                },
            )
    }
}

enum class WarehouseLocationSettingsAction {
    APP_SETTINGS,
    LOCATION_SETTINGS,
}

data class SettingItem(
    val title: String,
    val subtitle: String,
)
