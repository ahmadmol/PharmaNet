package com.pharmalink.feature.admin.ui.facility

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonSize
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSwitch
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.FacilityType

@Composable
fun CreateFacilityScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    onPickLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateFacilityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar(
                message = when (uiState.facilityType) {
                    FacilityType.PHARMACY -> "تم إنشاء الصيدلية بنجاح"
                    FacilityType.WAREHOUSE -> "تم إنشاء المستودع بنجاح"
                }
            )
            onSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error)
        }
    }

    CreateFacilityContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onFacilityTypeChange = viewModel::onFacilityTypeChange,
        onNameChange = viewModel::onNameChange,
        onAddressChange = viewModel::onAddressChange,
        onPhoneChange = viewModel::onPhoneChange,
        onLicenseNumberChange = viewModel::onLicenseNumberChange,
        onMapPickerClick = onPickLocation,
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
    onFacilityTypeChange: (FacilityType) -> Unit,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLicenseNumberChange: (String) -> Unit,
    onMapPickerClick: () -> Unit,
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
                                .background(MaterialTheme.colorScheme.primary),
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
                        containerColor = Color.White,
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
            // Facility Type Segmented Selector
            FacilityTypeSelector(
                selectedType = uiState.facilityType,
                onTypeSelected = onFacilityTypeChange,
            )

            // Main Form Card
            PharmaCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                elevationDp = 6f,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    // Facility Name
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

                    // Address
                    PharmaTextField(
                        value = uiState.address,
                        onValueChange = onAddressChange,
                        label = "الموقع الجغرافي",
                        placeholder = "حدد العنوان بالتفصيل",
                        leadingIcon = Icons.Outlined.LocationOn,
                        errorMessage = uiState.addressError,
                    )

                    // Phone Number
                    PharmaTextField(
                        value = uiState.phone,
                        onValueChange = onPhoneChange,
                        label = "رقم التواصل",
                        placeholder = "05XXXXXXXX",
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        errorMessage = uiState.phoneError,
                    )

                    // License Number
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

            // Map Picker Card
            MapPickerCard(
                hasCoordinates = uiState.latitude != null && uiState.longitude != null,
                latitude = uiState.latitude,
                longitude = uiState.longitude,
                onMapPickerClick = onMapPickerClick,
            )

            // Status Card
            StatusCard(
                isActive = uiState.isActive,
                onActiveToggle = onActiveToggle,
            )

            // Create Button
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
    val d = MaterialTheme.dimens
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
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
private fun MapPickerCard(
    hasCoordinates: Boolean,
    latitude: Double?,
    longitude: Double?,
    onMapPickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onMapPickerClick,
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasCoordinates) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (hasCoordinates) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(48.dp),
                )
                
                if (hasCoordinates && latitude != null && longitude != null) {
                    // Show coordinates when selected
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = d.spaceL,
                                    vertical = d.spaceM,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "تم تحديد الموقع",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        
                        // Display coordinates
                        Text(
                            text = "%.6f, %.6f".format(latitude, longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        
                        Text(
                            text = "اضغط لتغيير الموقع",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    // Show prompt to select location
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = d.spaceL,
                                vertical = d.spaceM,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "تحديد الموقع على الخريطة",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
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
            Column(
                modifier = Modifier.weight(1f),
            ) {
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
                address = "الرياض، حي النخيل",
                phone = "0501234567",
                licenseNumber = "PH-2024-001",
                latitude = null,
                longitude = null,
                isActive = true,
            ),
            onBackClick = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onMapPickerClick = {},
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
                address = "الرياض، حي النخيل",
                phone = "0501234567",
                licenseNumber = "PH-2024-001",
                latitude = 24.713600,
                longitude = 46.675300,
                isActive = true,
            ),
            onBackClick = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onMapPickerClick = {},
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
                address = "جدة، حي الصناعية",
                phone = "0507654321",
                licenseNumber = "WH-2024-001",
                isActive = true,
            ),
            onBackClick = {},
            onFacilityTypeChange = {},
            onNameChange = {},
            onAddressChange = {},
            onPhoneChange = {},
            onLicenseNumberChange = {},
            onMapPickerClick = {},
            onActiveToggle = {},
            onCreateClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
