package com.pharmalink.feature.profile

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens

private data class SettingsGroup(
    val title: String,
    val subtitle: String,
    val items: List<SettingItem>,
)

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onOpenHelpSupport: () -> Unit = {},
    onOpenAboutApp: () -> Unit = {},
    onOpenContactUs: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ProfileContent(
            uiState = uiState,
            onLogout = onLogout,
            onEditProfile = onEditProfile,
            onChangePassword = onChangePassword,
            onOpenHelpSupport = onOpenHelpSupport,
            onOpenAboutApp = onOpenAboutApp,
            onOpenContactUs = onOpenContactUs,
            onOpenLanguage = onOpenLanguage,
            onNotificationsToggle = viewModel::updateNotifications,
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenAboutApp: () -> Unit,
    onOpenContactUs: () -> Unit,
    onOpenLanguage: () -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
) {
    val d = MaterialTheme.dimens
    val isPublicUser = uiState.accountTypeEnum == com.pharmalink.domain.model.AccountType.PUBLIC_USER
    val context = LocalContext.current
    val settingsGroups = uiState.settingsOptions
        .filterNot { isPublicUser && it.isNotification(context) }
        .groupForProfile(uiState.accountTypeEnum, context)
    val languagePreview = stringResource(R.string.profile_language_arabic_only)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
        contentPadding = PaddingValues(bottom = d.spaceXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item { ProfileHeader() }
        item {
            ProfileHeroSection(
                userName = uiState.userName,
                accountType = uiState.accountType,
                accountTypeEnum = uiState.accountTypeEnum,
                pharmacyName = uiState.pharmacyName,
                pharmacyAddress = uiState.pharmacyAddress,
                isPublicUser = isPublicUser,
                onEditProfile = onEditProfile,
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        item {
            ProfileSummaryCard(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        items(settingsGroups) { group ->
            ProfileSettingsSection(
                group = group,
                languagePreview = languagePreview,
                onLanguageClick = onOpenLanguage,
                onSecurityClick = onChangePassword,
                onHelpSupportClick = onOpenHelpSupport,
                onAboutClick = onOpenAboutApp,
                onContactClick = onOpenContactUs,
                notificationsEnabled = uiState.notificationsEnabled,
                notificationsError = uiState.notificationsError,
                onNotificationsClick = { onNotificationsToggle(!uiState.notificationsEnabled) },
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        item {
            ProfileLogoutButton(
                onLogout = onLogout,
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        item {
            ProfileFooter(
                text = uiState.settingsOptions
                    .firstOrNull { it.isAbout(context) }
                    ?.subtitle
                    .orEmpty(),
            )
        }
    }

}

@Composable
private fun ProfileHeader() {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.profile_header_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfileHeroSection(
    userName: String,
    accountType: String,
    accountTypeEnum: com.pharmalink.domain.model.AccountType?,
    pharmacyName: String,
    pharmacyAddress: String,
    isPublicUser: Boolean,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val context = LocalContext.current
    val isWarehouse = accountTypeEnum == com.pharmalink.domain.model.AccountType.WAREHOUSE
    val organizationFallback = if (isWarehouse) {
        stringResource(R.string.edit_profile_role_warehouse)
    } else {
        accountType
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Box {
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(3.dp, PharmaBlue500),
                shadowElevation = d.cardElevation,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_picture_description),
                        tint = PharmaBlue500,
                        modifier = Modifier.size(62.dp),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(34.dp),
                shape = CircleShape,
                color = PharmaBlue500,
                contentColor = Color.White,
                border = BorderStroke(4.dp, ClinicalCanvas),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(d.iconS),
                    )
                }
            }
        }

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (isPublicUser) accountType else pharmacyName.ifBlank { organizationFallback },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = PharmaBlue500,
            textAlign = TextAlign.Center,
        )
        if (!isPublicUser && pharmacyAddress.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(d.iconXS),
                )
                Text(
                    text = pharmacyAddress,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StitchButton(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth(0.72f),
            contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceS),
        ) {
            Text(text = stringResource(R.string.edit_profile_button), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val isPublicUser = uiState.accountTypeEnum == com.pharmalink.domain.model.AccountType.PUBLIC_USER
    val isWarehouse = uiState.accountTypeEnum == com.pharmalink.domain.model.AccountType.WAREHOUSE
    val organizationLabel = when {
        isPublicUser -> stringResource(R.string.profile_org_label_user)
        isWarehouse -> stringResource(R.string.profile_org_label_warehouse)
        uiState.accountTypeEnum == com.pharmalink.domain.model.AccountType.PHARMACY -> stringResource(R.string.profile_org_label_pharmacy)
        else -> stringResource(R.string.profile_org_label_fallback)
    }
    val accountTypeLabel = if (isWarehouse) {
        stringResource(R.string.edit_profile_role_warehouse)
    } else {
        uiState.accountType
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.Transparent,
        shadowElevation = d.cardElevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PharmaBlue500, PharmaBlue700),
                    ),
                    RoundedCornerShape(d.radiusXXL),
                )
                .padding(d.spaceL),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(d.spaceL),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(R.string.profile_summary_account_type),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text(
                            text = accountTypeLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White,
                    ) {
                        Icon(
                            Icons.Outlined.Store,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(d.spaceS)
                                .size(d.iconM),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    SummaryInfo(
                        label = organizationLabel,
                        value = if (isPublicUser) uiState.pharmacyAddress else uiState.pharmacyName,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryInfo(
                        label = stringResource(R.string.profile_summary_phone),
                        value = uiState.userPhone,
                        modifier = Modifier.weight(1f),
                    )
                }
                SummaryInfo(
                    label = stringResource(R.string.profile_summary_email),
                    value = uiState.userEmail,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SummaryInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
        )
        Text(
            value.ifBlank { "-" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileSettingsSection(
    group: SettingsGroup,
    languagePreview: String,
    onLanguageClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onHelpSupportClick: () -> Unit,
    onAboutClick: () -> Unit,
    onContactClick: () -> Unit,
    notificationsEnabled: Boolean,
    notificationsError: String?,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
        horizontalAlignment = Alignment.Start,
    ) {
        Column(modifier = Modifier.padding(horizontal = d.spaceS), horizontalAlignment = Alignment.Start) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = group.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(d.radiusXL),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)),
        ) {
            Column(
                modifier = Modifier.padding(vertical = d.spaceXS),
                verticalArrangement = Arrangement.spacedBy(d.spaceXXS),
            ) {
                group.items.forEachIndexed { index, item ->
                    ProfileSettingRow(
                        item = item,
                        isLast = index == group.items.lastIndex,
                        languagePreview = languagePreview,
                        onClick = when {
                            item.isLanguage(context) -> onLanguageClick
                            item.isSecurity(context) -> onSecurityClick
                            item.isAbout(context) -> onAboutClick
                            item.isContact(context) -> onContactClick
                            item.isHelpSupport(context) -> onHelpSupportClick
                            item.isNotification(context) -> onNotificationsClick
                            else -> ({})
                        },
                        notificationsEnabled = notificationsEnabled,
                    )
                }
                if (!notificationsError.isNullOrBlank()) {
                    Text(
                        text = notificationsError,
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumUrgent,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingRow(
    item: SettingItem,
    isLast: Boolean,
    languagePreview: String,
    onClick: () -> Unit,
    notificationsEnabled: Boolean,
) {
    val d = MaterialTheme.dimens
    val context = LocalContext.current
    val icon = settingIcon(item.title, context)
    val isLanguage = item.isLanguage(context)
    val badgeText = when {
        isLanguage -> languagePreview
        item.isAbout(context) -> stringResource(R.string.badge_about)
        item.isNotification(context) -> if (notificationsEnabled) stringResource(R.string.badge_notification_on) else stringResource(R.string.badge_notification_off)
        item.isHelpSupport(context) -> stringResource(R.string.badge_help_support)
        item.isContact(context) -> stringResource(R.string.badge_contact)
        item.isSecurity(context) -> stringResource(R.string.badge_security)
        else -> stringResource(R.string.badge_default)
    }
    val badgeColor = when {
        isLanguage || item.isNotification(context) -> PharmaSuccess
        item.isHelpSupport(context) || item.isContact(context) || item.isSecurity(context) -> PharmaBlue500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onClick,
                    )
                    .padding(horizontal = d.spaceM, vertical = d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(d.radiusL),
                    color = if (isLanguage) PharmaBlue500.copy(alpha = 0.12f) else PharmaBlue50,
                    contentColor = PharmaBlue500,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(d.iconS))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = if (isLanguage) {
                                stringResource(R.string.profile_language_subtitle_with_preview, languagePreview)
                            } else {
                                item.subtitle
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.12f),
                    contentColor = badgeColor,
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(d.iconS),
                )
            }
            if (!isLast) {
                Spacer(
                    modifier = Modifier
                        .padding(start = d.spaceM + 44.dp + d.spaceM, end = d.spaceM)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
                )
            }
        }
    }
}

@Composable
private fun ProfileLogoutButton(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = PremiumUrgent,
        border = BorderStroke(1.dp, PremiumUrgent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = PremiumUrgent),
                    onClick = onLogout,
                )
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(d.iconS))
            Spacer(Modifier.size(d.spaceS))
            Text(
                text = stringResource(R.string.logout_button),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = PharmaNeutral600.copy(alpha = 0.72f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.dimens.spaceM),
    )
}

private fun List<SettingItem>.groupForProfile(accountType: com.pharmalink.domain.model.AccountType?, context: Context): List<SettingsGroup> {
    val general = filter { it.isNotification(context) || it.isLanguage(context) }
    val security = filter { it.isSecurity(context) }
    val support = if (accountType == com.pharmalink.domain.model.AccountType.ADMIN) {
        emptyList()
    } else {
        filter { it.isHelpSupport(context) || it.isContact(context) }
    }
    val grouped = general + security + support
    val fallback = filterNot { it in grouped }

    return buildList {
        if (general.isNotEmpty()) {
            add(SettingsGroup(
                title = context.getString(R.string.settings_group_general_title),
                subtitle = context.getString(R.string.settings_group_general_subtitle),
                items = general))
        }
        if (security.isNotEmpty()) {
            add(SettingsGroup(
                title = context.getString(R.string.settings_group_security_title),
                subtitle = context.getString(R.string.settings_group_security_subtitle),
                items = security))
        }
        if (support.isNotEmpty()) {
            add(SettingsGroup(
                title = context.getString(R.string.settings_group_support_title),
                subtitle = context.getString(R.string.settings_group_support_subtitle),
                items = support))
        }
        if (fallback.isNotEmpty()) {
            add(SettingsGroup(
                title = context.getString(R.string.settings_group_fallback_title),
                subtitle = context.getString(R.string.settings_group_fallback_subtitle),
                items = fallback))
        }
    }
}

private fun settingIcon(title: String, context: Context): ImageVector = when {
    SettingItem(title, "").isNotification(context) -> Icons.Outlined.Notifications
    SettingItem(title, "").isLanguage(context) -> Icons.Outlined.Language
    SettingItem(title, "").isSecurity(context) -> Icons.Outlined.Lock
    SettingItem(title, "").isHelpSupport(context) -> Icons.Outlined.HelpOutline
    SettingItem(title, "").isContact(context) -> Icons.Outlined.Phone
    SettingItem(title, "").isAbout(context) -> Icons.Outlined.Info
    else -> Icons.Outlined.Settings
}

private fun SettingItem.isNotification(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_notification)) ||
    title.contains(context.getString(R.string.keyword_notification_plural))

private fun SettingItem.isLanguage(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_language))

private fun SettingItem.isSecurity(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_security_aman)) ||
    title.contains(context.getString(R.string.keyword_security_khulos)) ||
    title.contains(context.getString(R.string.keyword_security_password)) ||
    title.contains(context.getString(R.string.keyword_security_aman_alt)) ||
    title.contains(context.getString(R.string.keyword_security_himayah)) ||
    title.contains(context.getString(R.string.keyword_security_sirya))

private fun SettingItem.isHelpSupport(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_help_moaan)) ||
    title.contains(context.getString(R.string.keyword_help_daam))

private fun SettingItem.isContact(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_contact))

private fun SettingItem.isAbout(context: Context): Boolean =
    title.contains(context.getString(R.string.keyword_about))

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    StitchTheme {
        ProfileContent(
            uiState = ProfileUiState(),
            onLogout = {},
            onEditProfile = {},
            onChangePassword = {},
            onOpenHelpSupport = {},
            onOpenAboutApp = {},
            onOpenContactUs = {},
            onOpenLanguage = {},
            onNotificationsToggle = {},
        )
    }
}
