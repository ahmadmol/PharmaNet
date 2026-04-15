package com.pharmalink.feature.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.dimens

import androidx.compose.runtime.DisposableEffect

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

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
                message = "تم تحديث الملف الشخصي بنجاح"
                isSuccess = true
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
            AccountTopBar(title = "تعديل الملف الشخصي", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.dimens.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
            ) {
                AvatarEditor(name = name)
                AccountSection(title = "الهوية المهنية") {
                    AccountTextField(value = name, onValueChange = { name = it }, label = "الاسم الكامل", icon = Icons.Outlined.Person)
                    AccountTextField(value = pharmacy, onValueChange = { pharmacy = it }, label = "اسم الصيدلية", icon = Icons.Outlined.Store)
                }
                AccountSection(title = "بيانات التواصل والموقع") {
                    AccountTextField(value = phone, onValueChange = { phone = it }, label = "رقم الهاتف", icon = Icons.Outlined.Phone, keyboardType = KeyboardType.Phone)
                    AccountTextField(value = email, onValueChange = { email = it }, label = "البريد الإلكتروني", icon = Icons.Outlined.Email, keyboardType = KeyboardType.Email, enabled = false)
                    AccountTextField(value = address, onValueChange = { address = it }, label = "العنوان بالتفصيل", icon = Icons.Outlined.LocationOn, minLines = 3)
                }
                Surface(
                    shape = CircleShape,
                    color = PharmaSuccess.copy(alpha = 0.12f),
                    contentColor = PharmaSuccess,
                ) {
                    Text(
                        text = "حالة الحساب: نشط",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
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
                        viewModel.updateProfile(name, pharmacy, phone, address)
                    },
                    modifier = Modifier.weight(1f),
                    isLoading = updateStatus is ProfileUpdateStatus.Loading,
                    contentPadding = PaddingValues(vertical = MaterialTheme.dimens.spaceM),
                ) {
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
                    color = PharmaBlue50,
                    contentColor = PharmaBlue500,
                    onClick = onBack,
                ) {
                    Text(
                        text = "إلغاء",
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
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "رجوع", tint = PharmaBlue500)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "الصيدلية الذكية",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PharmaBlue500,
            )
        }
    }
}

@Composable
private fun AvatarEditor(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Surface(
                modifier = Modifier.size(128.dp),
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier.background(Brush.linearGradient(listOf(PharmaBlue500, MaterialTheme.colorScheme.primaryContainer))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(name.ifBlank { "الملف الشخصي" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = PharmaBlue500)
        Text("صيدلي معتمد", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
            focusedBorderColor = PharmaBlue500.copy(alpha = 0.18f),
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
