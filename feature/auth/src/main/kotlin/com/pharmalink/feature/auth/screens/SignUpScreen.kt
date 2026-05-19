package com.pharmalink.feature.auth.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.domain.model.AccountType
import com.pharmalink.feature.auth.R
import com.pharmalink.feature.auth.SignUpUiState
import com.pharmalink.feature.auth.components.AuthLinkRow
import com.pharmalink.feature.auth.components.LocationTextField
import com.pharmalink.feature.auth.components.NameTextField
import com.pharmalink.feature.auth.components.PasswordTextField
import com.pharmalink.feature.auth.components.PharmacyTextField
import com.pharmalink.feature.auth.components.StitchAccountTypeSelector
import com.pharmalink.feature.auth.components.StitchAmbientBackground
import com.pharmalink.feature.auth.components.StitchGradientPrimaryButton
import com.pharmalink.feature.auth.components.StitchPhoneRow
import com.pharmalink.feature.auth.components.StitchStepChip
import com.pharmalink.feature.auth.components.WarehouseNameTextField

/**
 * Sign-up screen with account-type branching and role-specific fields.
 */
@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onAccountTypeChange: (AccountType) -> Unit,
    onFullNameChange: (String) -> Unit,
    onPharmacyNameChange: (String) -> Unit,
    onPharmacyLocationChange: (String) -> Unit,
    onWarehouseNameChange: (String) -> Unit,
    onWarehouseLocationChange: (String) -> Unit,
    onRequestCurrentLocationClick: () -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val scroll = rememberScrollState()

    val isFullNameError = uiState.fullName.isBlank()
    val isPharmacyNameError = uiState.pharmacyName.isBlank() && uiState.accountType == AccountType.PHARMACY
    val isPhoneError = uiState.phoneNumber.isBlank()
    val isPasswordError = uiState.password.isBlank()
    val isConfirmPasswordError = uiState.confirmPassword.isBlank() || uiState.password != uiState.confirmPassword
    val isWarehouseNameError = uiState.warehouseName.isBlank() && uiState.accountType == AccountType.WAREHOUSE
    val isWarehouseLocationError = uiState.warehouseLocation.isBlank() && uiState.accountType == AccountType.WAREHOUSE

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = d.spaceL),
    ) {
        StitchAmbientBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = d.spaceXL, bottom = d.spaceXXL),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Icon(
                        painter = painterResource(id = DsR.drawable.sydaliti_logo_full),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = stringResource(R.string.auth_brand_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.size(d.spaceXL))
            }

            Spacer(Modifier.height(d.spaceXL))

            Text(
                text = stringResource(R.string.auth_signup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(d.spaceS))
            Text(
                text = stringResource(R.string.signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(d.spaceXL))

            StitchStepChip(text = stringResource(R.string.signup_step_1_account_type))
            Spacer(Modifier.height(d.spaceM))
            StitchAccountTypeSelector(
                selected = uiState.accountType,
                onSelect = onAccountTypeChange,
            )

            Spacer(Modifier.height(d.spaceXL))

            StitchStepChip(text = stringResource(R.string.signup_step_2_personal_info))
            Spacer(Modifier.height(d.spaceM))

            Surface(
                shape = RoundedCornerShape(d.radiusL),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    NameTextField(
                        value = uiState.fullName,
                        onValueChange = onFullNameChange,
                        isError = isFullNameError,
                        errorMessage = if (isFullNameError) stringResource(R.string.error_full_name_required) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StitchPhoneRow(
                        fullPhoneNumber = uiState.phoneNumber,
                        onFullPhoneNumberChange = onPhoneNumberChange,
                        isError = isPhoneError,
                        errorMessage = if (isPhoneError) stringResource(R.string.error_phone_required) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        isError = isPasswordError,
                        errorMessage = if (isPasswordError) stringResource(R.string.error_password_required) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = stringResource(R.string.field_label_confirm_password),
                        isError = isConfirmPasswordError,
                        errorMessage = when {
                            uiState.confirmPassword.isEmpty() -> stringResource(R.string.error_confirm_password_required)
                            uiState.password != uiState.confirmPassword -> stringResource(R.string.error_password_mismatch_detail)
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (uiState.accountType == AccountType.PHARMACY) {
                Spacer(Modifier.height(d.spaceL))
                StitchStepChip(text = stringResource(R.string.signup_step_3_pharmacy_details))
                Spacer(Modifier.height(d.spaceM))
                Surface(
                    shape = RoundedCornerShape(d.radiusL),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        PharmacyTextField(
                            value = uiState.pharmacyName,
                            onValueChange = onPharmacyNameChange,
                            isError = isPharmacyNameError,
                            errorMessage = if (isPharmacyNameError) stringResource(R.string.error_pharmacy_name_required) else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocationTextField(
                            value = uiState.pharmacyLocation,
                            onValueChange = onPharmacyLocationChange,
                            label = stringResource(R.string.field_label_location_pharmacy),
                            isError = false,
                            errorMessage = null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = onRequestCurrentLocationClick,
                            enabled = !uiState.isResolvingLocation,
                        ) {
                            Text(stringResource(R.string.location_picker_use_current_location))
                        }
                        if (uiState.latitude != null && uiState.longitude != null) {
                            Text(
                                text = stringResource(
                                    R.string.location_picker_lat_lng_format,
                                    uiState.latitude,
                                    uiState.longitude
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (uiState.accountType == AccountType.WAREHOUSE) {
                Spacer(Modifier.height(d.spaceL))
                StitchStepChip(text = stringResource(R.string.signup_step_3_warehouse_details))
                Spacer(Modifier.height(d.spaceM))
                Surface(
                    shape = RoundedCornerShape(d.radiusL),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        WarehouseNameTextField(
                            value = uiState.warehouseName,
                            onValueChange = onWarehouseNameChange,
                            isError = isWarehouseNameError,
                            errorMessage = if (isWarehouseNameError) stringResource(R.string.error_warehouse_name_required) else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocationTextField(
                            value = uiState.warehouseLocation,
                            onValueChange = onWarehouseLocationChange,
                            label = stringResource(R.string.field_label_location_warehouse),
                            isError = isWarehouseLocationError,
                            errorMessage = if (isWarehouseLocationError) stringResource(R.string.error_warehouse_location_required) else null,
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                        )
                        TextButton(
                            onClick = onRequestCurrentLocationClick,
                            enabled = !uiState.isResolvingLocation,
                        ) {
                            Text(stringResource(R.string.location_picker_use_current_location))
                        }
                        if (uiState.latitude != null && uiState.longitude != null) {
                            Text(
                                text = stringResource(
                                    R.string.location_picker_lat_lng_format,
                                    uiState.latitude,
                                    uiState.longitude
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(d.spaceXL))

            StitchGradientPrimaryButton(
                text = stringResource(R.string.signup_create_account),
                onClick = onSignUpClick,
                enabled = isFormValid(uiState),
                isLoading = uiState.isLoading,
                showTrailingIcon = true,
            )

            uiState.successMessage?.let { success ->
                Spacer(Modifier.height(d.spaceM))
                Surface(
                    shape = RoundedCornerShape(d.radiusM),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = success,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(d.spaceM),
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                Spacer(Modifier.height(d.spaceM))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            uiState.locationMessage?.let { message ->
                Spacer(Modifier.height(d.spaceM))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.locationMessageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(d.spaceXL))

            AuthLinkRow(
                prefixText = stringResource(R.string.signup_have_account),
                linkText = stringResource(R.string.signup_login),
                onLinkClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(d.spaceL))
            Text(
                text = stringResource(R.string.signup_copyright),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun isFormValid(uiState: SignUpUiState): Boolean {
    val base = uiState.fullName.isNotEmpty() &&
        uiState.phoneNumber.isNotEmpty() &&
        uiState.password.isNotEmpty() &&
        uiState.confirmPassword.isNotEmpty() &&
        uiState.password == uiState.confirmPassword

    return when (uiState.accountType) {
        AccountType.PUBLIC_USER -> base
        AccountType.PHARMACY -> base && uiState.pharmacyName.isNotEmpty()
        AccountType.WAREHOUSE -> base &&
            uiState.warehouseName.isNotEmpty() &&
            uiState.warehouseLocation.isNotEmpty() &&
            uiState.latitude != null &&
            uiState.longitude != null
        AccountType.ADMIN -> base
    }
}
