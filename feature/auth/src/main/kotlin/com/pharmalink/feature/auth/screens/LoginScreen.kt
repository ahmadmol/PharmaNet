package com.pharmalink.feature.auth.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.auth.R
import com.pharmalink.domain.model.LoginUiState
import com.pharmalink.feature.auth.components.AuthSecondaryTextAction
import com.pharmalink.feature.auth.components.StitchAmbientBackground
import com.pharmalink.feature.auth.components.StitchGradientPrimaryButton
import com.pharmalink.feature.auth.components.StitchOrDivider
import com.pharmalink.feature.auth.components.StitchPasswordRow
import com.pharmalink.feature.auth.components.StitchPhoneRow

/**
 * Login screen — Stitch "Smart Pharmacy Auth Flow" layout (phone + password, guest, trust row).
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onPhoneNumberChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val scroll = rememberScrollState()
    // Real-time validation errors (independent of errorMessage)
    val phoneEmpty = uiState.phoneNumber.isBlank()
    val passEmpty = uiState.password.isBlank()

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
                .padding(vertical = d.spaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(d.spaceL))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(d.radiusL),
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(d.spaceL)
                            .size(40.dp),
                    )
                }
                Spacer(Modifier.height(d.spaceM))
                Text(
                    text = stringResource(R.string.auth_brand_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.auth_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(d.spaceL))
                Text(
                    text = stringResource(R.string.auth_welcome_back),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(d.spaceXXL))

            Surface(
                shape = RoundedCornerShape(d.radiusL),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    StitchPhoneRow(
                        fullPhoneNumber = uiState.phoneNumber,
                        onFullPhoneNumberChange = onPhoneNumberChange,
                        isError = phoneEmpty,
                        errorMessage = if (phoneEmpty) "رقم الهاتف مطلوب" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StitchPasswordRow(
                        password = uiState.password,
                        onPasswordChange = onPasswordChange,
                        isError = passEmpty,
                        errorMessage = if (passEmpty) "كلمة المرور مطلوبة" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AuthSecondaryTextAction(
                            text = stringResource(R.string.auth_forgot_password),
                            onClick = onForgotPasswordClick,
                        )
                    }
                    StitchGradientPrimaryButton(
                        text = stringResource(R.string.auth_login_button),
                        onClick = onLoginClick,
                        enabled = uiState.phoneNumber.isNotEmpty() && uiState.password.isNotEmpty(),
                        isLoading = uiState.isLoading,
                    )
                    uiState.errorMessage?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = stringResource(R.string.auth_create_account),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(onClick = onSignUpClick)
                            .padding(vertical = d.spaceXS),
                    )
                    if (onGuestClick != null) {
                        StitchOrDivider(Modifier.padding(vertical = d.spaceS))
                        Text(
                            text = stringResource(R.string.auth_or_via),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.height(d.spaceS))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onGuestClick)
                                .padding(vertical = d.spaceS),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.auth_google),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                            Icon(
                                Icons.Outlined.Fingerprint,
                                contentDescription = stringResource(R.string.auth_cd_biometric),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = stringResource(R.string.auth_biometric),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(d.spaceXXL))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                ) {
                    Row(
                        Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "دمشق • حلب، سوريا",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    TrustMiniTile(
                        icon = Icons.Outlined.LocalShipping,
                        text = "توصيل طبي فائق السرعة",
                        modifier = Modifier.weight(1f),
                    )
                    TrustMiniTile(
                        icon = Icons.Outlined.VerifiedUser,
                        text = "منصة طبية معتمدة",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(d.spaceXXL))
        }
    }
}

@Composable
private fun TrustMiniTile(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Surface(
                shape = RoundedCornerShape(d.radiusS),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(d.spaceXS)
                        .size(20.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
