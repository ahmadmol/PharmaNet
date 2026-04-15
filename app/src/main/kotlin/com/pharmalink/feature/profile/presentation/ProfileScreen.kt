package com.pharmalink.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.PharmacyProfile

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenHelp: () -> Unit = {},
    onOpenCompliance: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.profile_title),
        modifier = modifier,
        navigationContentDescription = stringResource(R.string.common_back),
        actions = {
            IconButton(onClick = onOpenNotifications) {
                BadgedBox(
                    badge = {
                        val success = (state.screenState as? com.pharmalink.core.common.ui.ScreenState.Success)?.data
                        val unread = success?.unreadNotifications ?: 0
                        if (unread > 0) {
                            Badge {
                                Text(if (unread > 9) "9+" else unread.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.profile_open_notifications),
                    )
                }
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
        ) {
        PharmaScreenState(
            screenState = state.screenState,
            loading = PharmaStateSpec(
                title = stringResource(R.string.profile_title),
                subtitle = stringResource(R.string.profile_loading_subtitle),
                tone = PharmaStateTone.Loading,
            ),
            empty = PharmaStateSpec(
                title = stringResource(R.string.profile_title),
                subtitle = stringResource(R.string.profile_error_subtitle),
            ),
            error = PharmaStateSpec(
                title = stringResource(R.string.profile_title),
                subtitle = stringResource(R.string.profile_error_subtitle),
                tone = PharmaStateTone.Error,
            ),
            offline = PharmaStateSpec(
                title = stringResource(R.string.profile_title),
                subtitle = stringResource(R.string.profile_error_subtitle),
                tone = PharmaStateTone.Offline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceXL),
        ) { content ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
                verticalArrangement = Arrangement.spacedBy(d.spaceL),
            ) {
                item {
                    ProfileHeader(
                        content = content,
                        onOpenCompliance = onOpenCompliance,
                    )
                }
                item {
                    SummarySection(content)
                }
                item {
                    InfoSection(
                        title = stringResource(R.string.profile_section_pharmacy_info),
                        subtitle = stringResource(R.string.profile_subtitle),
                        rows = listOf(
                            InfoRow(Icons.Outlined.PersonOutline, stringResource(R.string.profile_manager_label), content.profile.managerName),
                            InfoRow(Icons.Outlined.LocationOn, stringResource(R.string.profile_address_label), content.profile.addressLine),
                            InfoRow(Icons.Outlined.Phone, stringResource(R.string.profile_phone_label), content.profile.contactPhone),
                            InfoRow(Icons.Outlined.Email, stringResource(R.string.profile_email_label), content.profile.contactEmail),
                        ),
                    )
                }
                item {
                    InfoSection(
                        title = stringResource(R.string.profile_section_operations),
                        rows = listOf(
                            InfoRow(Icons.Outlined.LocationOn, stringResource(R.string.profile_city_district_label), "${content.profile.city} - ${content.profile.district}"),
                            InfoRow(Icons.Outlined.Shield, stringResource(R.string.profile_license_status_label), content.profile.licenseStatusLabel),
                            InfoRow(Icons.Outlined.Shield, stringResource(R.string.profile_license_number_label), content.profile.licenseNumber),
                            InfoRow(Icons.Outlined.Shield, stringResource(R.string.profile_license_expiry_label), content.profile.licenseExpiryLabel),
                            InfoRow(Icons.Outlined.Smartphone, stringResource(R.string.profile_operating_hours_label), content.profile.operatingHoursLabel),
                        ),
                    )
                }
                item {
                    SecuritySection(content.profile)
                }
                item {
                    PreferencesSection(
                        profile = content.profile,
                        unreadNotifications = content.unreadNotifications,
                        isUpdating = state.isUpdatingNotifications,
                        updateError = state.notificationsUpdateError,
                        onNotificationsToggle = viewModel::updateNotifications,
                        onOpenNotifications = onOpenNotifications,
                    )
                }
                item {
                    SupportSection(
                        onOpenHelp = onOpenHelp,
                        onOpenCompliance = onOpenCompliance,
                    )
                }
                item {
                    PharmaButton(
                        text = stringResource(R.string.profile_logout_action),
                        onClick = onLogout,
                        style = PharmaButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ProfileHeader(
    content: ProfileContent,
    onOpenCompliance: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val complianceTone = if (content.complianceAlertsCount == 0 && content.documentsNeedingAttentionCount == 0) {
        StatusTone.Success
    } else {
        StatusTone.Warning
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreen)
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_avatar_placeholder),
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = content.profile.pharmacyName,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${content.profile.district} - ${content.profile.city}",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = content.profile.managerName,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PharmaStatusChip(
                    label = content.compliance.licenseStatusLabel,
                    tone = complianceTone,
                )
            }

            Text(
                text = "${stringResource(R.string.profile_license_number_label)}: ${content.profile.licenseNumber}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${stringResource(R.string.profile_license_expiry_label)}: ${content.profile.licenseExpiryLabel}",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                    if (content.unreadNotifications > 0) {
                        PharmaStatusChip(
                            label = "${content.unreadNotifications} ${stringResource(R.string.profile_summary_notifications)}",
                            tone = StatusTone.Pending,
                        )
                    }
                    if (content.documentsNeedingAttentionCount > 0) {
                        PharmaStatusChip(
                            label = "${content.documentsNeedingAttentionCount} ${stringResource(R.string.profile_summary_documents)}",
                            tone = StatusTone.Warning,
                        )
                    }
                }
                TextButton(onClick = onOpenCompliance) {
                    Text(
                        text = stringResource(R.string.profile_open_compliance),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySection(content: ProfileContent) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = stringResource(R.string.profile_section_summary))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            PharmaOutlinedTile(
                title = stringResource(R.string.profile_summary_orders_total),
                value = content.profile.totalOrders.toString(),
                modifier = Modifier.weight(1f),
            )
            PharmaOutlinedTile(
                title = stringResource(R.string.profile_summary_orders_active),
                value = content.profile.activeOrders.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            PharmaOutlinedTile(
                title = stringResource(R.string.profile_summary_notifications),
                value = content.unreadNotifications.toString(),
                modifier = Modifier.weight(1f),
            )
            PharmaOutlinedTile(
                title = stringResource(R.string.profile_summary_compliance),
                value = content.complianceAlertsCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SecuritySection(profile: PharmacyProfile) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = stringResource(R.string.profile_section_security))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                SettingRow(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.profile_setting_two_factor_title),
                    subtitle = if (profile.twoFactorEnabled) {
                        stringResource(R.string.profile_two_factor_enabled)
                    } else {
                        stringResource(R.string.profile_two_factor_disabled)
                    },
                    badgeLabel = if (profile.twoFactorEnabled) {
                        stringResource(R.string.profile_status_on)
                    } else {
                        stringResource(R.string.profile_status_off)
                    },
                    badgeTone = if (profile.twoFactorEnabled) StatusTone.Success else StatusTone.Warning,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.Smartphone,
                    title = stringResource(R.string.profile_setting_devices_title),
                    subtitle = stringResource(R.string.profile_security_devices_summary, profile.linkedDevicesCount),
                    badgeLabel = profile.linkedDevicesCount.toString(),
                    badgeTone = StatusTone.Neutral,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.profile_security_session_title),
                    subtitle = stringResource(R.string.profile_security_session_subtitle),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.Phone,
                    title = stringResource(R.string.profile_security_contact_title),
                    subtitle = stringResource(R.string.profile_security_contact_subtitle),
                    badgeLabel = stringResource(R.string.profile_status_on),
                    badgeTone = StatusTone.Success,
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    profile: PharmacyProfile,
    unreadNotifications: Int,
    isUpdating: Boolean,
    updateError: String?,
    onNotificationsToggle: (Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = stringResource(R.string.profile_section_app_preferences))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                SettingRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = stringResource(R.string.profile_setting_push_title),
                    subtitle = if (profile.notificationsEnabled) {
                        stringResource(R.string.profile_notifications_enabled)
                    } else {
                        stringResource(R.string.profile_notifications_disabled)
                    },
                    trailing = {
                        Switch(
                            checked = profile.notificationsEnabled,
                            onCheckedChange = onNotificationsToggle,
                            enabled = !isUpdating,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.profile_language_title),
                    subtitle = stringResource(R.string.profile_language_summary),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = stringResource(R.string.profile_notifications_center_title),
                    subtitle = if (unreadNotifications > 0) {
                        stringResource(R.string.profile_notifications_unread_summary, unreadNotifications)
                    } else {
                        stringResource(R.string.profile_notifications_clear_summary)
                    },
                    actionLabel = stringResource(R.string.common_open_details),
                    onClick = onOpenNotifications,
                )
                updateError?.let { message ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceM),
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportSection(
    onOpenHelp: () -> Unit,
    onOpenCompliance: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(
            title = stringResource(R.string.profile_section_support),
            subtitle = stringResource(R.string.profile_support_status),
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                SettingRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = stringResource(R.string.profile_support_help_title),
                    subtitle = stringResource(R.string.profile_support_help_summary),
                    actionLabel = stringResource(R.string.common_open_details),
                    onClick = onOpenHelp,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.profile_support_compliance_title),
                    subtitle = stringResource(R.string.profile_support_compliance_summary),
                    actionLabel = stringResource(R.string.common_open_details),
                    onClick = onOpenCompliance,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SettingRow(
                    icon = Icons.Outlined.ReportProblem,
                    title = stringResource(R.string.profile_support_ticket_title),
                    subtitle = stringResource(R.string.profile_support_ticket_summary),
                    actionLabel = stringResource(R.string.common_open_details),
                    onClick = onOpenHelp,
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    subtitle: String? = null,
    rows: List<InfoRow>,
) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(
            title = title,
            subtitle = subtitle,
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    SettingRow(
                        icon = row.icon,
                        title = row.title,
                        subtitle = row.value,
                    )
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    badgeLabel: String? = null,
    badgeTone: StatusTone = StatusTone.Neutral,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (onClick != null) {
                    role = Role.Button
                }
            }
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(d.spaceL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            trailing != null -> trailing()
            badgeLabel != null -> PharmaStatusChip(label = badgeLabel, tone = badgeTone)
            actionLabel != null -> Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class InfoRow(
    val icon: ImageVector,
    val title: String,
    val value: String,
)
