package com.pharmalink.feature.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType
import com.pharmalink.feature.auth.R
import com.pharmalink.feature.auth.SignUpUiState
import com.pharmalink.feature.auth.components.AuthLinkRow
import com.pharmalink.feature.auth.components.LocationPickerComponent
import com.pharmalink.feature.auth.components.LocationTextField
import com.pharmalink.feature.auth.components.NameTextField
import com.pharmalink.feature.auth.components.PasswordStrengthMeter
import com.pharmalink.feature.auth.components.PasswordTextField
import com.pharmalink.feature.auth.components.PharmacyTextField
import com.pharmalink.feature.auth.components.StitchAccountTypeSelector
import com.pharmalink.feature.auth.components.StitchAmbientBackground
import com.pharmalink.feature.auth.components.StitchGradientPrimaryButton
import com.pharmalink.feature.auth.components.StitchPhoneRow
import com.pharmalink.feature.auth.components.StitchStepChip
import com.pharmalink.feature.auth.components.TermsAndConditionsRow
import com.pharmalink.feature.auth.components.WarehouseNameTextField
import kotlinx.coroutines.launch

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
    onAgreedToTermsChange: (Boolean) -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    var showLocationPicker by remember { mutableStateOf(false) }

    val activeFacilityLocation = when (uiState.accountType) {
        AccountType.PHARMACY -> uiState.pharmacyLocation
        AccountType.WAREHOUSE -> uiState.warehouseLocation
        else -> ""
    }
    
    val openLocationPickerAndRequest = {
        showLocationPicker = true
        onRequestCurrentLocationClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = d.spaceL),
    ) {
        StitchAmbientBackground(Modifier.fillMaxSize())
        
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(d.spaceXL))
            
            // Header with Back Navigation for Pager
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(onClick = { 
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } 
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.auth_brand_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(d.spaceL))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> StepAccountType(
                        selectedType = uiState.accountType,
                        onTypeSelect = {
                            onAccountTypeChange(it)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> StepPersonalInfo(
                        uiState = uiState,
                        onFullNameChange = onFullNameChange,
                        onPhoneNumberChange = onPhoneNumberChange,
                        onPasswordChange = onPasswordChange,
                        onConfirmPasswordChange = onConfirmPasswordChange,
                        onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> StepFacilityLocation(
                        uiState = uiState,
                        onPharmacyNameChange = onPharmacyNameChange,
                        onPharmacyLocationChange = onPharmacyLocationChange,
                        onWarehouseNameChange = onWarehouseNameChange,
                        onWarehouseLocationChange = onWarehouseLocationChange,
                        onAgreedToTermsChange = onAgreedToTermsChange,
                        openLocationPickerAndRequest = openLocationPickerAndRequest,
                        onSignUpClick = onSignUpClick,
                        onLoginClick = onLoginClick
                    )
                }
            }
        }
    }

    if (showLocationPicker && uiState.accountType in setOf(AccountType.PHARMACY, AccountType.WAREHOUSE)) {
        LocationPickerComponent(
            selectedAddress = activeFacilityLocation,
            initialLatitude = uiState.latitude,
            initialLongitude = uiState.longitude,
            isResolvingLocation = uiState.isResolvingLocation,
            locationMessage = uiState.locationMessage,
            locationMessageIsError = uiState.locationMessageIsError,
            onDismiss = { showLocationPicker = false },
            onUseCurrentLocation = onRequestCurrentLocationClick,
        )
    }
}

@Composable
private fun StepAccountType(
    selectedType: AccountType,
    onTypeSelect: (AccountType) -> Unit,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "إنشاء حساب جديد",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "اختر نوع الحساب المناسب لك للمتابعة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(d.spaceXL))
        StitchStepChip(text = stringResource(R.string.signup_step_1_account_type))
        Spacer(Modifier.height(d.spaceM))
        StitchAccountTypeSelector(
            selected = selectedType,
            onSelect = onTypeSelect,
        )
    }
}

@Composable
private fun StepPersonalInfo(
    uiState: SignUpUiState,
    onFullNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val isFullNameError = uiState.fullName.isBlank()
    val isPhoneError = uiState.phoneNumber.isBlank()
    val isPasswordError = uiState.password.isBlank()
    val isConfirmPasswordError = uiState.confirmPassword.isNotBlank() && uiState.password != uiState.confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceL)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "المعلومات الشخصية",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "أدخل بياناتك الأساسية لتأمين حسابك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StitchStepChip(text = stringResource(R.string.signup_step_2_personal_info))

        Surface(
            shape = RoundedCornerShape(d.radiusL),
            shadowElevation = 2.dp,
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
                Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        isError = isPasswordError,
                        errorMessage = if (isPasswordError) stringResource(R.string.error_password_required) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordStrengthMeter(password = uiState.password)
                }
                PasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.field_label_confirm_password),
                    isError = isConfirmPasswordError,
                    errorMessage = if (isConfirmPasswordError) stringResource(R.string.error_password_mismatch_detail) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        StitchGradientPrimaryButton(
            text = "المتابعة",
            onClick = onNext,
            enabled = uiState.fullName.isNotBlank() && uiState.phoneNumber.isNotBlank() && uiState.password.isNotBlank() && uiState.password == uiState.confirmPassword
        )
    }
}

@Composable
private fun StepFacilityLocation(
    uiState: SignUpUiState,
    onPharmacyNameChange: (String) -> Unit,
    onPharmacyLocationChange: (String) -> Unit,
    onWarehouseNameChange: (String) -> Unit,
    onWarehouseLocationChange: (String) -> Unit,
    onAgreedToTermsChange: (Boolean) -> Unit,
    openLocationPickerAndRequest: () -> Unit,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val isPharmacyNameError = uiState.pharmacyName.isBlank() && uiState.accountType == AccountType.PHARMACY
    val isWarehouseNameError = uiState.warehouseName.isBlank() && uiState.accountType == AccountType.WAREHOUSE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceL)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (uiState.accountType == AccountType.PUBLIC_USER) "إتمام التسجيل" else "معلومات المنشأة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "حدد موقع ونوع نشاطك لإتمام العملية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.accountType != AccountType.PUBLIC_USER && uiState.accountType != AccountType.ADMIN) {
            StitchStepChip(text = "الخطوة الأخيرة: تفاصيل المنشأة")

            Surface(
                shape = RoundedCornerShape(d.radiusL),
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    if (uiState.accountType == AccountType.PHARMACY) {
                        PharmacyTextField(
                            value = uiState.pharmacyName,
                            onValueChange = onPharmacyNameChange,
                            isError = isPharmacyNameError,
                            errorMessage = if (isPharmacyNameError) stringResource(R.string.error_pharmacy_name_required) else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocationField(
                            location = uiState.pharmacyLocation,
                            onLocationChange = onPharmacyLocationChange,
                            onClick = openLocationPickerAndRequest,
                            isLoading = uiState.isResolvingLocation
                        )
                    } else if (uiState.accountType == AccountType.WAREHOUSE) {
                        WarehouseNameTextField(
                            value = uiState.warehouseName,
                            onValueChange = onWarehouseNameChange,
                            isError = isWarehouseNameError,
                            errorMessage = if (isWarehouseNameError) stringResource(R.string.error_warehouse_name_required) else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocationField(
                            location = uiState.warehouseLocation,
                            onLocationChange = onWarehouseLocationChange,
                            onClick = openLocationPickerAndRequest,
                            isLoading = uiState.isResolvingLocation
                        )
                    }
                }
            }
        }

        TermsAndConditionsRow(
            checked = uiState.agreedToTerms,
            onCheckedChange = onAgreedToTermsChange
        )

        StitchGradientPrimaryButton(
            text = stringResource(R.string.signup_create_account),
            onClick = onSignUpClick,
            enabled = isFormValid(uiState),
            isLoading = uiState.isLoading,
        )

        AnimatedVisibility(visible = uiState.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = uiState.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        AuthLinkRow(
            prefixText = stringResource(R.string.signup_have_account),
            linkText = stringResource(R.string.signup_login),
            onLinkClick = onLoginClick,
        )
    }
}

@Composable
private fun LocationField(
    location: String,
    onLocationChange: (String) -> Unit,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Box(Modifier.fillMaxWidth()) {
        LocationTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "موقع المنشأة",
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = !isLoading, onClick = onClick),
        )
    }
}

private fun isFormValid(uiState: SignUpUiState): Boolean {
    val base = uiState.fullName.isNotBlank() &&
        uiState.phoneNumber.isNotBlank() &&
        uiState.password.isNotBlank() &&
        uiState.password == uiState.confirmPassword &&
        uiState.agreedToTerms

    return when (uiState.accountType) {
        AccountType.PUBLIC_USER -> base
        AccountType.PHARMACY -> base && uiState.pharmacyName.isNotBlank() && uiState.latitude != null
        AccountType.WAREHOUSE -> base && uiState.warehouseName.isNotBlank() && uiState.latitude != null
        AccountType.ADMIN -> base
    }
}
