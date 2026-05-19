package com.pharmalink.feature.admin.ui.audit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.RemoveCircleOutline
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.admin.R
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun AuditLogDetailScreen(
    onBack: () -> Unit,
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
            AuditLogDetailTopBar(onBack = onBack, title = stringResource(R.string.audit_log_detail_title))
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
        )
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            MainInfoCard(auditLog = auditLog)
            TargetEntityCard(auditLog = auditLog)
            JsonDiffRow(auditLog = auditLog)
            TechnicalDetailsCard(auditLog = auditLog)
        }
    }
}

@Composable
private fun AuditLogDetailTopBar(
    onBack: () -> Unit,
    title: String,
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
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(d.iconM),
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
private fun JsonDiffRow(auditLog: AuditLogDetailModel, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    val highlight = remember(auditLog.oldValue, auditLog.newValue) {
        collectChangedPrimitiveContents(auditLog.oldValue, auditLog.newValue)
    }
    val maxH = d.iconXXL + d.iconXXL + d.iconXXL + d.iconXXL + d.iconXXL + d.iconXXL
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        JsonValueCard(
            title = stringResource(R.string.audit_log_old_value_title),
            raw = auditLog.oldValue,
            highlightValues = emptySet(),
            headerContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            headerContentColor = MaterialTheme.colorScheme.error,
            icon = Icons.Outlined.RemoveCircleOutline,
            modifier = Modifier
                .weight(1f)
                .heightIn(max = maxH),
        )
        JsonValueCard(
            title = stringResource(R.string.audit_log_new_value_title),
            raw = auditLog.newValue,
            highlightValues = highlight,
            headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            headerContentColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Outlined.AddCircleOutline,
            modifier = Modifier
                .weight(1f)
                .heightIn(max = maxH),
        )
    }
}

@Composable
private fun JsonValueCard(
    title: String,
    raw: String,
    highlightValues: Set<String>,
    headerContainerColor: Color,
    headerContentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Surface(
                color = headerContainerColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(d.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = headerContentColor,
                        modifier = Modifier.size(d.iconM),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = headerContentColor,
                    )
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(d.spaceM),
            ) {
                HighlightedJsonText(
                    raw = raw,
                    highlightPrimitiveContents = highlightValues,
                )
            }
        }
    }
}

@Composable
private fun HighlightedJsonText(
    raw: String,
    highlightPrimitiveContents: Set<String>,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val fg = MaterialTheme.colorScheme.primary
    val text = remember(raw, highlightPrimitiveContents, bg, fg) {
        buildHighlightedJsonAnnotatedString(raw, highlightPrimitiveContents, bg, fg)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = d.spaceXXS,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(d.spaceM),
        )
    }
}

private fun buildHighlightedJsonAnnotatedString(
    raw: String,
    highlightPrimitiveContents: Set<String>,
    highlightBg: Color,
    highlightFg: Color,
): AnnotatedString {
    if (highlightPrimitiveContents.isEmpty()) {
        return AnnotatedString(raw)
    }
    return buildAnnotatedString {
        append(raw)
        val sorted = highlightPrimitiveContents
            .filter { it.isNotEmpty() }
            .sortedByDescending { it.length }
        val style = SpanStyle(background = highlightBg, color = highlightFg)
        for (token in sorted) {
            var start = raw.indexOf(token)
            while (start >= 0) {
                val before = raw.getOrNull(start - 1)
                val after = raw.getOrNull(start + token.length)
                val boundaryBefore = before == null || before in setOf(':', '[', ',', '{', ' ', '\n', '\"', '\'')
                val boundaryAfter = after == null || after in setOf(',', '}', ']', ' ', '\n', '\"', '\'')
                if (boundaryBefore && boundaryAfter) {
                    addStyle(style, start, start + token.length)
                }
                start = raw.indexOf(token, start + 1)
            }
        }
    }
}

@Composable
private fun TechnicalDetailsCard(auditLog: AuditLogDetailModel, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    val dash = stringResource(R.string.placeholder_em_dash)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
    ) {
        Column(Modifier.padding(d.spaceXXL)) {
            Text(
                text = stringResource(R.string.audit_log_technical_section_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(d.spaceL))
            TechnicalRow(
                label = stringResource(R.string.audit_log_ip_label),
                value = auditLog.ipAddress ?: dash,
                showDivider = true,
            )
            TechnicalRow(
                label = stringResource(R.string.audit_log_user_agent_label),
                value = auditLog.userAgent ?: dash,
                showDivider = true,
            )
            TechnicalRow(
                label = stringResource(R.string.audit_log_transaction_label),
                value = auditLog.transactionId ?: dash,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun TechnicalRow(
    label: String,
    value: String,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = d.spaceM),
            )
        }
        if (showDivider) {
            Spacer(Modifier.height(d.spaceM))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            Spacer(Modifier.height(d.spaceM))
        }
    }
}

private fun collectChangedPrimitiveContents(oldRaw: String, newRaw: String): Set<String> {
    val oldEl = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(oldRaw) }.getOrNull()
        ?: return emptySet()
    val newEl = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(newRaw) }.getOrNull()
        ?: return emptySet()
    val changed = mutableSetOf<String>()
    diffJsonLeaves(oldEl, newEl, changed)
    return changed
}

private fun diffJsonLeaves(a: JsonElement, b: JsonElement, out: MutableSet<String>) {
    when {
        a is JsonObject && b is JsonObject -> {
            val keys = a.keys + b.keys
            for (k in keys) {
                diffJsonLeaves(a[k] ?: JsonNull, b[k] ?: JsonNull, out)
            }
        }
        a is kotlinx.serialization.json.JsonArray && b is kotlinx.serialization.json.JsonArray -> {
            val n = maxOf(a.size, b.size)
            for (i in 0 until n) {
                val ae = if (i < a.size) a[i] else JsonNull
                val be = if (i < b.size) b[i] else JsonNull
                diffJsonLeaves(ae, be, out)
            }
        }
        else -> {
            if (a != b && b is JsonPrimitive) {
                val c = b.contentOrNull ?: return
                out.add(c)
            }
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
                oldValue = """{"quantity": "1000", "price": "80.00"}""",
                newValue = """{"quantity": "1200", "price": "82.50"}""",
                ipAddress = "192.168.1.104",
                userAgent = "Chrome v118 (Windows 11)",
                transactionId = "TRX-7729-AX",
            ),
            onBack = {},
        )
    }
}
