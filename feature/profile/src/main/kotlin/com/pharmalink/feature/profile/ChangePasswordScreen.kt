package com.pharmalink.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val hasMismatch = uiState.confirmPassword.isNotBlank() && uiState.newPassword != uiState.confirmPassword

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
        ) {
            AccountTopBar(title = "تغيير كلمة المرور", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.dimens.spaceL),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
                horizontalAlignment = Alignment.Start,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("أمان الحساب", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = PharmaBlue500)
                    Text(
                        "قم بتحديث كلمة المرور الخاصة بك لحماية حسابك ومعلوماتك الحساسة.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MaterialTheme.dimens.radiusXXL),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
                    ) {
                        PasswordField("كلمة المرور الحالية", uiState.currentPassword, viewModel::updateCurrentPassword, Icons.Outlined.Lock, uiState.passwordsVisible, viewModel::togglePasswordVisibility)
                        PasswordField("كلمة المرور الجديدة", uiState.newPassword, viewModel::updateNewPassword, Icons.Outlined.Key, uiState.passwordsVisible, viewModel::togglePasswordVisibility)
                        PasswordField("تأكيد كلمة المرور الجديدة", uiState.confirmPassword, viewModel::updateConfirmPassword, Icons.Outlined.VerifiedUser, uiState.passwordsVisible, viewModel::togglePasswordVisibility)
                        PasswordRules(next = uiState.newPassword, confirmMatches = !hasMismatch && uiState.confirmPassword.isNotBlank())
                        val message = uiState.errorMessage ?: uiState.successMessage
                        message?.let {
                            InfoCallout(text = it)
                        }
                        if (hasMismatch) {
                            Surface(
                                shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ) {
                                Text(
                                    "كلمة المرور الجديدة وتأكيدها غير متطابقين.",
                                    modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        StitchButton(
                            onClick = viewModel::changePassword,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(vertical = MaterialTheme.dimens.spaceM),
                        ) {
                            Text(if (uiState.isLoading) "جارٍ الحفظ..." else "حفظ التغييرات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
                    color = PharmaBlue50,
                    contentColor = PharmaBlue500,
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
                    ) {
                        Icon(Icons.Outlined.EnhancedEncryption, contentDescription = null)
                        Text(
                            "يتم تحديث كلمة المرور الجديدة بشكل آمن عبر المصادقة في Supabase.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    visible: Boolean,
    onVisibilityToggle: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onVisibilityToggle) {
                Icon(Icons.Outlined.Visibility, contentDescription = null)
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PharmaBlue500.copy(alpha = 0.18f),
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = PharmaBlue500,
        ),
    )
}

@Composable
private fun PasswordRules(next: String, confirmMatches: Boolean) {
    val rules = listOf(
        "على الأقل 8 أحرف" to (next.length >= 8),
        "حرف كبير واحد" to next.any { it.isUpperCase() },
        "رقم واحد على الأقل" to next.any { it.isDigit() },
        "كلمتا المرور متطابقتان" to confirmMatches,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXL),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = PharmaBlue500, modifier = Modifier.size(18.dp))
                Text("متطلبات كلمة المرور", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PharmaBlue500)
            }
            rules.forEach { (title, ok) ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (ok) PharmaSuccess else PremiumUrgent.copy(alpha = 0.35f), RoundedCornerShape(99.dp)),
                    )
                    Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
