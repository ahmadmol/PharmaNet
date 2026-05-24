package com.pharmalink.feature.admin.ui.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.admin.R
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarIcon

@Composable
fun AuditLogDetailScreen(
    onBack: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AuditLogDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        AuditLogDetailUiState.Loading -> AuditLogDetailLoading(modifier)
        is AuditLogDetailUiState.Error -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.dimens.spaceL),
        ) {
            AuditLogDetailTopBar(
                onBack = onBack,
                title = stringResource(R.string.audit_log_detail_title),
                profileImageUrl = profileImageUrl,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.spaceL))
            PharmaStateView(
                title = stringResource(R.string.audit_log_error_title),
                subtitle = state.message,
                tone = com.pharmalink.designsystem.components.PharmaStateTone.Error,
                actionLabel = stringResource(R.string.audit_log_error_retry),
                onAction = { viewModel.onAction(AuditLogDetailAction.OnRetryClicked) },
            )
        }
        is AuditLogDetailUiState.Success -> AuditLogDetailContent(
            auditLog = state.log,
            onBack = onBack,
            profileImageUrl = profileImageUrl,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuditLogDetailLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.spaceL),
    ) {
        PharmaSkeletonLine(heightDp = MaterialTheme.dimens.iconM.value)
        Spacer(Modifier.height(MaterialTheme.dimens.spaceL))
        repeat(8) {
            PharmaSkeletonLine()
            Spacer(Modifier.height(MaterialTheme.dimens.spaceM))
        }
    }
}

@Composable
private fun AuditLogDetailContent(
    auditLog: AuditLogDetailModel,
    onBack: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll),
    ) {
        AuditLogDetailTopBar(
            onBack = onBack,
            title = stringResource(R.string.audit_log_detail_title),
            profileImageUrl = profileImageUrl,
        )
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            AdminDetailSectionEntrance {
                MainInfoCard(auditLog = auditLog)
            }
            AdminDetailSectionEntrance {
                TargetEntityCard(auditLog = auditLog)
            }
            AdminDetailSectionEntrance {
                ChangedFieldsCard(auditLog = auditLog)
            }
        }
    }
}

@Composable
private fun AdminDetailSectionEntrance(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
            slideInVertically(animationSpec = tween(durationMillis = 220)) { it / 5 },
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
private fun AuditLogDetailTopBar(
    onBack: () -> Unit,
    title: String,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(modifier.fillMaxWidth()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceS, vertical = d.spaceXS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(d.space4XL)
                        .clip(CircleShape)
                        .border(
                            width = d.spaceXS,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AdminProfileAvatarIcon(
                        profileImageUrl = profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(d.space4XL),
                        fallbackSize = d.iconM,
                        fallbackTint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = d.spaceXXS,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun MainInfoCard(auditLog: AuditLogDetailModel, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
    ) {
        Column(Modifier.padding(d.spaceL)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        modifier = Modifier.size(d.space5XL),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.HistoryEdu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(d.iconL),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.audit_log_activity_type_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = auditLog.actionLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                val statusLabel = if (auditLog.isSuccess) {
                    stringResource(R.string.audit_log_status_success)
                } else {
                    stringResource(R.string.audit_log_status_failed)
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    )
                }
            }
            Spacer(Modifier.height(d.spaceL))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                InfoTile(
                    label = stringResource(R.string.audit_log_responsible_label),
                    value = auditLog.adminName,
                    modifier = Modifier.weight(1f),
                )
                InfoTile(
                    label = stringResource(R.string.audit_log_datetime_label),
                    value = auditLog.formattedDateTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(d.spaceL)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(d.spaceXS))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TargetEntityCard(auditLog: AuditLogDetailModel, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = d.fabElevation / 2),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(d.spaceL)) {
                Text(
                    text = stringResource(R.string.audit_log_affected_item_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = auditLog.targetEntityName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(d.spaceM))
                val warehouse = auditLog.targetWarehouseName
                if (!warehouse.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(d.iconM),
                        )
                        Text(
                            text = warehouse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                val sku = auditLog.targetSku
                if (!sku.isNullOrBlank()) {
                    Spacer(Modifier.height(d.spaceXS))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(d.iconM),
                        )
                        Text(
                            text = stringResource(R.string.audit_log_sku_prefix, sku),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Outlined.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(d.iconXXL + d.iconXXL),
            )
        }
    }
}

@Composable
private fun ChangedFieldsCard(auditLog: AuditLogDetailModel, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceXXL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = "التغييرات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (auditLog.changedFields.isEmpty()) {
                Text(
                    text = "لا توجد تغييرات قابلة للعرض.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                auditLog.changedFields.forEachIndexed { index, field ->
                    ChangedFieldRow(
                        field = field,
                        showDivider = index < auditLog.changedFields.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangedFieldRow(
    field: AuditChangedFieldModel,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(modifier.fillMaxWidth()) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(d.spaceS))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            ChangeValueTile(
                label = "السابق",
                value = field.oldValue,
                modifier = Modifier.weight(1f),
            )
            ChangeValueTile(
                label = "الجديد",
                value = field.newValue,
                modifier = Modifier.weight(1f),
            )
        }
        if (showDivider) {
            Spacer(Modifier.height(d.spaceM))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun ChangeValueTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(d.spaceM)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(d.spaceXS))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAuditLogDetailContent() {
    PharmaTheme {
        AuditLogDetailContent(
            auditLog = AuditLogDetailModel(
                actionLabel = "تعديل بيانات المخزون",
                isSuccess = true,
                adminName = "د. أحمد خالد",
                formattedDateTime = "12 أكتوبر 2023 - 07:45 AM",
                targetEntityName = "أوجمنتين 1 جم",
                targetWarehouseName = "مستودع الرياض المركزي",
                targetSku = "PH-99203",
                changedFields = listOf(
                    AuditChangedFieldModel(
                        label = "الكمية",
                        oldValue = "1000",
                        newValue = "1200",
                    ),
                    AuditChangedFieldModel(
                        label = "السعر",
                        oldValue = "80",
                        newValue = "82.5",
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
