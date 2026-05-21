package com.pharmalink.feature.profile
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType
import coil.compose.SubcomposeAsyncImage

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val context = LocalContext.current
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        selectedAvatarUri = uri
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.updateWarehouseLocationFromCurrentGps()
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null && permissions.keys.none { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            viewModel.onWarehouseLocationPermissionDenied(permanentlyDenied)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUpdateStatus()
        }
    }

    var name by remember { mutableStateOf(uiState.userName) }
    var pharmacy by remember { mutableStateOf(uiState.pharmacyName) }
    var phone by remember { mutableStateOf(uiState.userPhone) }
    var email by remember { mutableStateOf(uiState.userEmail) }
    var address by remember { mutableStateOf(uiState.pharmacyAddress) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val accountType = uiState.accountTypeEnum
    val isAdmin = accountType == AccountType.ADMIN
    val isPublicUser = accountType == AccountType.PUBLIC_USER
    val isPharmacy = accountType == AccountType.PHARMACY
    val isWarehouse = accountType == AccountType.WAREHOUSE
    val organizationNameLabel = stringResource(R.string.edit_profile_label_pharmacy_name)
    val organizationLocationLabel = when {
        isPublicUser -> stringResource(R.string.edit_profile_label_address_user)
        isWarehouse -> stringResource(R.string.edit_profile_label_warehouse_location)
        else -> stringResource(R.string.edit_profile_label_address)
    }
    val roleLabel = when {
        isAdmin -> uiState.accountType
        isPublicUser -> stringResource(R.string.edit_profile_role_user)
        isWarehouse -> stringResource(R.string.edit_profile_role_warehouse)
        else -> stringResource(R.string.edit_profile_role_pharmacy)
    }

    LaunchedEffect(uiState) {
        name = uiState.userName
        pharmacy = uiState.pharmacyName
        phone = uiState.userPhone
        email = uiState.userEmail
        address = uiState.pharmacyAddress
    }

    LaunchedEffect(updateStatus) {
        when (val status = updateStatus) {
            is ProfileUpdateStatus.Success -> {
                message = context.getString(R.string.edit_profile_success)
                isSuccess = true
                selectedAvatarUri = null
            }
            is ProfileUpdateStatus.Error -> {
                message = status.message
                isSuccess = false
            }
            else -> Unit
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
        ) {
            AccountTopBar(title = stringResource(R.string.edit_profile_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
            ) {
                AvatarEditor(
                    name = name,
                    roleLabel = roleLabel,
                    selectedAvatarUri = selectedAvatarUri,
                    profileImageUrl = uiState.profileImageUrl,
                    onPickAvatar = { avatarPickerLauncher.launch("image/*") },
                )
                AccountSection(title = if (isPharmacy) organizationNameLabel else stringResource(R.string.edit_profile_section_account_info)) {
                    AccountTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.edit_profile_label_full_name), icon = Icons.Outlined.Person)
                    if (isPharmacy) {
                        AccountTextField(value = pharmacy, onValueChange = { pharmacy = it }, label = organizationNameLabel, icon = Icons.Outlined.Store)
                    }
                }
                AccountSection(title = stringResource(R.string.edit_profile_section_contact_info)) {
                    AccountTextField(value = phone, onValueChange = { phone = it }, label = stringResource(R.string.edit_profile_label_phone), icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
                    AccountTextField(value = email, onValueChange = { email = it }, label = stringResource(R.string.edit_profile_label_email), icon = Icons.Outlined.Email, keyboardType = KeyboardType.Email, enabled = false)
                    if (isWarehouse) {
                        WarehouseLocationSection(
                            address = address,
                            latitude = uiState.warehouseLatitude,
                            longitude = uiState.warehouseLongitude,
                            isLoading = uiState.isUpdatingWarehouseLocation,
                            message = uiState.warehouseLocationMessage,
                            isError = uiState.warehouseLocationMessageIsError,
                            settingsAction = uiState.warehouseLocationSettingsAction,
                            onUseCurrentLocation = {
                                if (context.hasLocationPermission()) {
                                    viewModel.updateWarehouseLocationFromCurrentGps()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ),
                                    )
                                }
                            },
                            onOpenSettings = { action ->
                                context.openWarehouseLocationSettings(action)
                            },
                        )
                    } else if (isPublicUser || isPharmacy) {
                        AccountTextField(value = address, onValueChange = { address = it }, label = organizationLocationLabel, icon = Icons.Outlined.LocationOn, minLines = 3)
                    }
                }
                message?.let {
                    InfoCallout(text = it, isSuccess = isSuccess)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .padding(MaterialTheme.dimens.spaceL),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
            ) {
                StitchButton(
                    onClick = {
                        viewModel.updateProfile(
                            name = name,
                            pharmacy = if (isPharmacy) pharmacy else uiState.pharmacyName,
                            phone = phone,
                            address = if (isPublicUser || isPharmacy) address else uiState.pharmacyAddress,
                            avatarUri = selectedAvatarUri,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    isLoading = updateStatus is ProfileUpdateStatus.Loading,
                    contentPadding = PaddingValues(vertical = MaterialTheme.dimens.spaceM),
                ) {
                    Text(stringResource(R.string.edit_profile_save), fontWeight = FontWeight.Bold)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
                    color = PharmaBlue50,
                    contentColor = PharmaBlue500,
                    onClick = onBack,
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile_cancel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = MaterialTheme.dimens.spaceM),
                    )
                }
            }
        }
    }
}

@Composable
fun AccountTopBar(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.spaceL, vertical = MaterialTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = stringResource(R.string.edit_profile_back), tint = PharmaBlue500)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.edit_profile_account_type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PharmaBlue500,
            )
        }
    }
}

@Composable
private fun WarehouseLocationSection(
    address: String,
    latitude: Double?,
    longitude: Double?,
    isLoading: Boolean,
    message: String?,
    isError: Boolean,
    settingsAction: WarehouseLocationSettingsAction?,
    onUseCurrentLocation: () -> Unit,
    onOpenSettings: (WarehouseLocationSettingsAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
            color = PharmaBlue50.copy(alpha = 0.65f),
        ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = PharmaBlue500,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXS),
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile_label_warehouse_location),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = address.ifBlank { "لم يتم تحديد موقع المستودع بعد" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (latitude != null && longitude != null) {
                        Text(
                            text = stringResource(R.string.location_picker_lat_lng_format, latitude, longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        StitchButton(
            onClick = onUseCurrentLocation,
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
            enabled = !isLoading,
            contentPadding = PaddingValues(vertical = MaterialTheme.dimens.spaceM),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(Modifier.size(MaterialTheme.dimens.spaceS))
            }
            Text("استخدام موقعي الحالي", fontWeight = FontWeight.Bold)
        }

        message?.let {
            InfoCallout(text = it, isSuccess = !isError)
        }

        settingsAction?.let { action ->
            TextButton(
                onClick = { onOpenSettings(action) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when (action) {
                        WarehouseLocationSettingsAction.APP_SETTINGS -> "فتح إعدادات التطبيق"
                        WarehouseLocationSettingsAction.LOCATION_SETTINGS -> "فتح إعدادات الموقع"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AvatarEditor(
    name: String,
    roleLabel: String,
    selectedAvatarUri: Uri?,
    profileImageUrl: String?,
    onPickAvatar: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = CircleShape,
                color = Color.Transparent,
                onClick = onPickAvatar,
            ) {
                Box(
                    modifier = Modifier.background(Brush.linearGradient(listOf(PharmaBlue500, MaterialTheme.colorScheme.primaryContainer))),
                    contentAlignment = Alignment.Center,
                ) {
                    EditableAvatarImage(
                        selectedAvatarUri = selectedAvatarUri,
                        profileImageUrl = profileImageUrl,
                        modifier = Modifier.size(128.dp),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onPickAvatar,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(name.ifBlank { stringResource(R.string.edit_profile_label_full_name) }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PharmaBlue500)
        Text(roleLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditableAvatarImage(
    selectedAvatarUri: Uri?,
    profileImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val model = selectedAvatarUri ?: profileImageUrl?.takeIf { it.isNotBlank() }
    if (model != null) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
            loading = {
                EditableAvatarFallbackIcon()
            },
            error = {
                EditableAvatarFallbackIcon()
            },
        )
    } else {
        EditableAvatarFallbackIcon()
    }
}

@Composable
private fun EditableAvatarFallbackIcon() {
    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
}

private fun Context.hasLocationPermission(): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

private fun Context.openWarehouseLocationSettings(action: WarehouseLocationSettingsAction) {
    val intent = when (action) {
        WarehouseLocationSettingsAction.APP_SETTINGS -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        WarehouseLocationSettingsAction.LOCATION_SETTINGS -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AccountSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 26.dp)
                        .background(PharmaBlue500, RoundedCornerShape(99.dp)),
                )
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PharmaBlue500)
            }
            content()
        }
    }
}

@Composable
fun AccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        enabled = enabled,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PharmaBlue500.copy(alpha = 0.1f),
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = PharmaBlue50.copy(alpha = 0.65f),
            cursorColor = PharmaBlue500,
            disabledBorderColor = Color.Transparent,
            disabledContainerColor = PharmaBlue50.copy(alpha = 0.4f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        ),
    )
}

@Composable
fun InfoCallout(text: String, isSuccess: Boolean = true) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
        color = if (isSuccess) PharmaBlue50 else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
        contentColor = if (isSuccess) PharmaBlue500 else MaterialTheme.colorScheme.error,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.Person, // Fallback icon for error
                contentDescription = null
            )
            Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
