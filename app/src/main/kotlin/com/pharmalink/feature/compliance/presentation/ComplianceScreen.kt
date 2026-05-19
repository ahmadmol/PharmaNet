package com.pharmalink.feature.compliance.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.ComplianceDocument
import com.pharmalink.domain.model.ComplianceDocumentStatus
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.SupplierComplianceItem

@Composable
fun ComplianceScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComplianceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.compliance_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.common_back),
        modifier = modifier,
        actions = {
            TextButton(onClick = onOpenHelp) {
                Text(stringResource(R.string.compliance_help_action))
            }
        },
    ) {
        PharmaScreenState(
            screenState = state.screenState,
            loading = PharmaStateSpec(
                title = stringResource(R.string.compliance_title),
                subtitle = stringResource(R.string.compliance_loading_subtitle),
                tone = PharmaStateTone.Loading,
            ),
            empty = PharmaStateSpec(
                title = stringResource(R.string.compliance_title),
                subtitle = stringResource(R.string.compliance_error_subtitle),
            ),
            error = PharmaStateSpec(
                title = stringResource(R.string.compliance_title),
                subtitle = stringResource(R.string.compliance_error_subtitle),
                tone = PharmaStateTone.Error,
            ),
            offline = PharmaStateSpec(
                title = stringResource(R.string.compliance_title),
                subtitle = stringResource(R.string.compliance_error_subtitle),
                tone = PharmaStateTone.Offline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceXL),
        ) { overview ->
            ComplianceContent(overview = overview)
        }
    }
}

@Composable
private fun ComplianceContent(
    overview: ComplianceOverview,
) {
    val d = MaterialTheme.dimens
    val context = LocalContext.current
    val supplierAttentionCount = overview.supplierItems.count { it.requiresAttention(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            ComplianceHeader(
                overview = overview,
                supplierAttentionCount = supplierAttentionCount,
            )
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.compliance_alerts_title),
                subtitle = stringResource(R.string.compliance_alerts_subtitle),
            )
        }
        if (overview.alerts.isEmpty()) {
            item {
                PharmaStateView(
                    title = stringResource(R.string.compliance_no_alerts_title),
                    subtitle = stringResource(R.string.compliance_no_alerts_subtitle),
                    tone = PharmaStateTone.Success,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(overview.alerts, key = { it }) { alert ->
                ComplianceItemCard(
                    icon = Icons.Outlined.Schedule,
                    title = alert,
                    subtitle = stringResource(R.string.compliance_alert_followup_subtitle),
                    tone = StatusTone.Warning,
                    badgeLabel = stringResource(R.string.compliance_supplier_attention),
                )
            }
        }

        item {
            PharmaSectionHeader(
                title = stringResource(R.string.compliance_documents_title),
                subtitle = stringResource(R.string.compliance_documents_subtitle),
            )
        }
        items(overview.documents, key = { it.id }) { document ->
            DocumentCard(document)
        }
        item {
            ComplianceItemCard(
                icon = Icons.Outlined.UploadFile,
                title = stringResource(R.string.compliance_upload_center_title),
                subtitle = stringResource(R.string.compliance_upload_center_subtitle),
                tone = StatusTone.Pending,
                badgeLabel = stringResource(R.string.compliance_badge_placeholder),
            )
        }

        item {
            PharmaSectionHeader(
                title = stringResource(R.string.compliance_suppliers_title),
                subtitle = stringResource(R.string.compliance_suppliers_subtitle),
            )
        }
        items(overview.supplierItems, key = { it.id }) { item ->
            SupplierCard(item)
        }
    }
}

@Composable
private fun ComplianceHeader(
    overview: ComplianceOverview,
    supplierAttentionCount: Int,
) {
    val d = MaterialTheme.dimens
    val tone = if (overview.alerts.isEmpty()) StatusTone.Success else StatusTone.Warning

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.compliance_subtitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(d.spaceXS))
                    Text(
                        text = overview.summaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
                PharmaStatusChip(
                    label = overview.licenseStatusLabel,
                    tone = tone,
                )
            }

            Text(
                text = "${stringResource(R.string.compliance_license_number)}: ${overview.licenseNumber}",
                color = Color.White,
            )
            Text(
                text = "${stringResource(R.string.compliance_license_expiry)}: ${overview.licenseExpiryLabel}",
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                PharmaOutlinedTile(
                    title = stringResource(R.string.compliance_metric_alerts),
                    value = overview.alerts.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.compliance_metric_documents),
                    value = overview.documents.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.compliance_metric_suppliers),
                    value = supplierAttentionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(document: ComplianceDocument) {
    val (tone, actionLabel, footer) = when (document.status) {
        ComplianceDocumentStatus.VALID -> Triple(
            StatusTone.Success,
            stringResource(R.string.compliance_action_monitor_status),
            stringResource(R.string.compliance_document_valid_note),
        )
        ComplianceDocumentStatus.EXPIRING_SOON -> Triple(
            StatusTone.Warning,
            stringResource(R.string.compliance_action_prepare_renewal),
            document.note,
        )
        ComplianceDocumentStatus.MISSING -> Triple(
            StatusTone.Urgent,
            stringResource(R.string.compliance_action_complete_upload),
            document.note,
        )
    }

    ComplianceItemCard(
        icon = Icons.Outlined.Description,
        title = document.title,
        subtitle = "${document.expiryLabel}\n$footer",
        tone = tone,
        badgeLabel = document.statusLabel,
        footerActionLabel = actionLabel,
    )
}

@Composable
private fun SupplierCard(item: SupplierComplianceItem) {
    val context = LocalContext.current
    ComplianceItemCard(
        icon = Icons.Outlined.VerifiedUser,
        title = item.supplierName,
        subtitle = "${item.nextReviewLabel}\n${item.note}",
        tone = if (item.requiresAttention(context)) StatusTone.Warning else StatusTone.Success,
        badgeLabel = item.statusLabel,
        footerActionLabel = if (item.requiresAttention(context)) {
            stringResource(R.string.compliance_action_monitor_status)
        } else {
            null
        },
    )
}

@Composable
private fun ComplianceItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: StatusTone,
    badgeLabel: String,
    footerActionLabel: String? = null,
) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                androidx.compose.material3.Icon(
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
                PharmaStatusChip(
                    label = badgeLabel,
                    tone = tone,
                )
            }
            if (footerActionLabel != null) {
                Text(
                    text = footerActionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun SupplierComplianceItem.requiresAttention(context: Context): Boolean {
    return statusLabel.contains(context.getString(R.string.compliance_keyword_follow_up)) ||
           statusLabel.contains(context.getString(R.string.compliance_keyword_needs))
}
