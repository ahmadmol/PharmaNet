package com.pharmalink.feature.request

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Request

@Composable
fun RequestDetailsScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.request_details_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.request_details_back),
        modifier = modifier,
    ) {
        PharmaScreenState(
            screenState = state.screenState,
            loading = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_loading_subtitle),
                tone = PharmaStateTone.Loading,
            ),
            empty = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_empty_subtitle),
            ),
            error = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_error_subtitle),
                tone = PharmaStateTone.Error,
            ),
            offline = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_offline_subtitle),
                tone = PharmaStateTone.Offline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceXL),
        ) { request ->
            RequestDetailsContent(
                request = request,
                onOpenOrder = onOpenOrder,
            )
        }
    }
}

@Composable
private fun RequestDetailsContent(
    request: Request,
    onOpenOrder: (String) -> Unit,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            // Main Request Summary Card
            RequestSummaryCard(request = request)
        }
        
        item {
            RequestStatusTimeline(
                currentStatus = request.status,
                modifier = Modifier.padding(vertical = d.spaceM),
            )
        }
        
        item {
            WarehouseInfoCard(request = request)
        }
        
        item {
            StorageNotesCard(request = request)
        }
        
        item {
            EtaCard(request = request)
        }
        
        item {
            request.relatedOrderId?.let { orderId ->
                PharmaButton(
                    text = stringResource(R.string.request_details_open_related_order),
                    onClick = { onOpenOrder(orderId) },
                    style = PharmaButtonStyle.GradientAccent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RequestSummaryCard(request: Request) {
    val d = MaterialTheme.dimens
    
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            // Header with ID and Urgency Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.request_details_request_id_format, request.id),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                
                if (request.priority == com.pharmalink.domain.model.RequestPriority.URGENT) {
                    UrgencyBadge()
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Medicine Info with Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MedicineImagePlaceholder(
                    imageUrl = request.medicineImageUrl,
                    modifier = Modifier.padding(end = d.spaceM),
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = request.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    
                    if (request.medicineSubtitle.isNotEmpty()) {
                        Text(
                            text = request.medicineSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    
                    Spacer(Modifier.height(d.spaceS))
                    
                    Text(
                        text = "${request.quantity} ${request.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgencyBadge() {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = stringResource(R.string.request_details_urgent_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        },
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = PremiumUrgent,
        ),
    )
}

@Composable
private fun WarehouseInfoCard(request: Request) {
    val d = MaterialTheme.dimens
    
    InfoCard(
        title = stringResource(R.string.request_details_section_warehouse),
        icon = Icons.Outlined.LocalShipping,
        items = listOf(
            InfoItem(stringResource(R.string.request_details_label_warehouse), request.warehouseName),
            InfoItem(stringResource(R.string.request_details_label_supplier), request.supplierName),
            InfoItem(stringResource(R.string.request_details_label_last_updated), request.updatedAtLabel),
        ),
    )
}

@Composable
private fun StorageNotesCard(request: Request) {
    val d = MaterialTheme.dimens
    
    InfoCard(
        title = stringResource(R.string.request_details_section_storage),
        icon = Icons.Outlined.Notes,
        items = listOfNotNull(
            if (request.notes.isNotBlank()) {
                InfoItem(stringResource(R.string.request_details_label_order_notes), request.notes)
            } else {
                null
            },
            if (request.storageNotes.isNotBlank()) {
                InfoItem(stringResource(R.string.request_details_label_storage_notes), request.storageNotes)
            } else {
                null
            },
            InfoItem(
                stringResource(R.string.request_details_label_priority),
                if (request.priority == com.pharmalink.domain.model.RequestPriority.URGENT) {
                    stringResource(R.string.request_details_priority_urgent)
                } else {
                    stringResource(R.string.request_details_priority_normal)
                },
            ),
        ),
    )
}

@Composable
private fun EtaCard(request: Request) {
    val d = MaterialTheme.dimens
    
    if (request.etaLabel.isNotEmpty()) {
        InfoCard(
            title = stringResource(R.string.request_details_section_eta),
            icon = Icons.Outlined.Schedule,
            items = listOf(
                InfoItem(stringResource(R.string.request_details_label_expected_delivery), request.etaLabel),
            ),
        )
    }
}

@Composable
private fun ActionButtons(
    onContactWarehouse: () -> Unit,
    onReorder: () -> Unit,
    request: Request,
) {
    val d = MaterialTheme.dimens
    
    Column(
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Primary CTA - Contact Warehouse
        PharmaButton(
            text = stringResource(R.string.request_details_contact_warehouse),
            onClick = onContactWarehouse,
            style = PharmaButtonStyle.GradientAccent,
            modifier = Modifier.fillMaxWidth(),
        )
        
        // Secondary CTA - Reorder
        PharmaButton(
            text = stringResource(R.string.request_details_reorder_same),
            onClick = onReorder,
            style = PharmaButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<InfoItem>,
) {
    val d = MaterialTheme.dimens
    
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = d.spaceM),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(d.spaceS))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            
            // Items
            items.forEach { item ->
                Column(
                    modifier = Modifier.padding(vertical = d.spaceXS),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private data class InfoItem(
    val label: String,
    val value: String,
)

