package com.pharmalink.feature.admin.ui.facility

import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonSize
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSwitch
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.FacilityType
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateFacilityScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateFacilityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION,
        onPermissionResult = { isGranted ->
            if (isGranted) viewModel.requestCurrentLocation() else viewModel.onLocationPermissionDenied()
        },
    )

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is CreateFacilityEffect.ShowSuccess -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is CreateFacilityEffect.ShowError -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            CreateFacilityEffect.NavigateBack -> onSuccess()
        }
    }

    CreateFacilityContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToProfile = onNavigateToProfile,
        onFacilityTypeChange = viewModel::onFacilityTypeChange,
        onNameChange = viewModel::onNameChange,
        onAddressChange = viewModel::onAddressChange,
        onPhoneChange = viewModel::onPhoneChange,
        onLicenseNumberChange = viewModel::onLicenseNumberChange,
        onUseCurrentLocation = {
            if (locationPermissionState.status.isGranted) {
                viewModel.requestCurrentLocation()
            } else {
                locationPermissionState.launchPermissionRequest()
            }
        },
        onActiveToggle = viewModel::onActiveToggle,
        onCreateClick = viewModel::onCreateClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFacilityContent(
    uiState: CreateFacilityUiState,
    onBackClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onFacilityTypeChange: (FacilityType) -> Unit,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLicenseNumberChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onActiveToggle: (Boolean) -> Unit,
    onCreateClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "إضافة منشأة",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                )
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(),
                                    onClick = onNavigateToProfile,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.spaceL, vertical = d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            FacilityTypeSelector(
                selectedType = uiState.facilityType,
                onTypeSelected = onFacilityTypeChange,
            )

            PharmaCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                elevationDp = 6f,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                    PharmaTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = "اسم المنشأة",
                        placeholder = when (uiState.facilityType) {
                            FacilityType.PHARMACY -> "أدخل اسم الصيدلية"
                            FacilityType.WAREHOUSE -> "أدخل اسم المستودع"
                        },
                        leadingIcon = when (uiState.facilityType) {
                            FacilityType.PHARMACY -> Icons.Outlined.LocalPharmacy
                            FacilityType.WAREHOUSE -> Icons.Outlined.Warehouse
                        },
                        errorMessage = uiState.nameError,
                    )

                    PharmaTextField(
                        value = uiState.address,
                        onValueChange = onAddressChange,
                        label = "المنطقة",
                        placeholder = if (uiState.isResolvingLocation) {
                            "جاري تحديد المنطقة..."
                        } else {
                            "اضغط أيقونة الموقع لتعبئة المنطقة تلقائياً"
                        },
                        leadingIcon = Icons.Outlined.LocationOn,
                        trailingIcon = {
                            IconButton(
                                onClick = onUseCurrentLocation,
                                enabled = !uiState.isResolvingLocation,
                            ) {
                                if (uiState.isResolvingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = "تحديد الموقع الحالي",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        supportingText = when {
                            uiState.latitude != null && uiState.longitude != null -> {
                                String.format(
                                    Locale.US,
                                    "GPS: %.6f, %.6f",
                                    uiState.latitude,
                                    uiState.longitude,
                                )
                            }
                            else -> "سيتم استخدام GPS لتعبئة اسم المنطقة"
                        },
                        errorMessage = uiState.addressError,
                    )

                    PharmaTextField(
                        value = uiState.phone,
                        onValueChange = onPhoneChange,
                        label = "رقم التواصل",
                        placeholder = "05XXXXXXXX",
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        errorMessage = uiState.phoneError,
                    )

                    PharmaTextField(
                        value = uiState.licenseNumber,
                        onValueChange = onLicenseNumberChange,
                        label = "رقم الترخيص الطبي",
                        placeholder = "أدخل رقم الترخيص الساري",
                        leadingIcon = Icons.Outlined.Verified,
                        errorMessage = uiState.licenseError,
                    )
                }
            }

            LocationSummaryCard(
                hasCoordinates = uiState.latitude != null && uiState.longitude != null,
                latitude = uiState.latitude,
                longitude = uiState.longitude,
            )

            StatusCard(
                isActive = uiState.isActive,
                onActiveToggle = onActiveToggle,
            )

            PharmaButton(
                text = when (uiState.facilityType) {
                    FacilityType.PHARMACY -> "إنشاء الصيدلية"
                    FacilityType.WAREHOUSE -> "إنشاء المستودع"
                },
                onClick = onCreateClick,
                enabled = !uiState.isSubmitting,
                size = PharmaButtonSize.Large,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FacilityTypeSelector(
    selectedType: FacilityType,
    onTypeSelected: (FacilityType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(d.spaceXS),
            horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            FacilityTypeTab(
                text = "صيدلية جديدة",
                isSelected = selectedType == FacilityType.PHARMACY,
                onClick = { onTypeSelected(FacilityType.PHARMACY) },
                modifier = Modifier.weight(1f),
            )
            FacilityTypeTab(
                text = "مستودع جديد",
                isSelected = selectedType == FacilityType.WAREHOUSE,
                onClick = { onTypeSelected(FacilityType.WAREHOUSE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FacilityTypeTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(300),
        label = "tab_background",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "tab_text",
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = tween(300),
        label = "tab_elevation",
    )

    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun LocationSummaryCard(
    hasCoordinates: Boolean,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasCoordinates) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = if (hasCoordinates) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasCoordinates) "تم التقاط موقع المنشأة" else "لم يتم التقاط موقع GPS بعد",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = if (hasCoordinates && latitude != null && longitude != null) {
                        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
                    } else {
                        "استخدم أيقونة الموقع داخل حقل المنطقة"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isActive: Boolean,
    onActiveToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "حالة المنشأة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = "تفعيل المنشأة فور الإنشاء",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PharmaSwitch(
                checked = isActive,
                onCheckedChange = onActiveToggle,
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CreateFacilityScreenPreview() {
    PharmaTheme {
        CreateFacilityContent(
            uiState = CreateFacilityUiState(
                facilityType = FacilityType.PHARMACY,
                name = "صيدلية النور",
                address = "حي النخيل",
                phone = "0501234567",
                licenseNumber = "PH-2024-001",
                isActive = true,
            ),
            onBackClick = {},
            onNavigateToProfile = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onUseCurrentLocation = {},
            onActiveToggle = {},
            onCreateClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CreateFacilityWithLocationPreview() {
    PharmaTheme {
        CreateFacilityContent(
            uiState = CreateFacilityUiState(
                facilityType = FacilityType.PHARMACY,
                name = "صيدلية النور",
                address = "حي النخيل",
                phone = "0501234567",
                licenseNumber = "PH-2024-001",
                latitude = 24.713600,
                longitude = 46.675300,
                isActive = true,
            ),
            onBackClick = {},
            onNavigateToProfile = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onUseCurrentLocation = {},
            onActiveToggle = {},
            onCreateClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun CreateWarehouseScreenPreview() {
    PharmaTheme {
        CreateFacilityContent(
            uiState = CreateFacilityUiState(
                facilityType = FacilityType.WAREHOUSE,
                name = "مستودع الدواء المركزي",
                address = "المنطقة الصناعية",
                phone = "0507654321",
                licenseNumber = "WH-2024-001",
                isActive = true,
            ),
            onBackClick = {},
            onNavigateToProfile = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onUseCurrentLocation = {},
            onActiveToggle = {},
            onCreateClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
