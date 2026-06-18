package com.pharmalink.feature.request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.feature.request.R
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.RequestPriority
import androidx.compose.material.icons.filled.FlashOn
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PremiumUrgent
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Surface
import com.pharmalink.domain.model.RequestItem


@Composable
fun RequestListScreen(
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToRequestDetails: (String) -> Unit,
    viewModel: RequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPharmacyUser = uiState.accountType == AccountType.PHARMACY
    val d = MaterialTheme.dimens
    val headerTitle = if (uiState.accountType == AccountType.WAREHOUSE) {
        stringResource(R.string.request_list_incoming_title)
    } else {
        stringResource(R.string.request_list_title)
    }

    LaunchedEffect(uiState.errorMessage) {
        // Handle error display if needed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimens.spaceM)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isPharmacyUser) {
                Surface(
                    onClick = onNavigateToCreateRequest,
                    shape = RoundedCornerShape(d.radiusM),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = "طلب جديد", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceM))

        // Status Filter
        RequestStatusFilter(
            selectedStatus = uiState.selectedStatus,
            onStatusSelected = viewModel::filterByStatus
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceM))

        // Error State
        uiState.errorMessage?.let { error ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceM))
                
                Button(
                    onClick = { viewModel.refreshRequests() }
                ) {
                    Text(stringResource(R.string.request_retry_loading))
                }
            }
        }
        
        // Content
        when {
            uiState.isLoading && uiState.requests.isEmpty() -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) { // Show 5 shimmer items
                        RequestItemCardPlaceholder()
                    }
                }
            }
            
            uiState.requests.isEmpty() && !uiState.isLoading && uiState.errorMessage == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.request_list_empty),
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.request_list_empty),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPharmacyUser) {
                                stringResource(R.string.request_empty_subtitle)
                            } else {
                                stringResource(R.string.request_empty_incoming_subtitle)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (isPharmacyUser) {
                            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceM))

                            Button(
                                onClick = { onNavigateToCreateRequest() }
                            ) {
                                Text(stringResource(R.string.request_create_first))
                            }
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)
                ) {
                    // Loading indicator at top when refreshing
                    if (uiState.isLoading && uiState.requests.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.dimens.spaceM),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    items(uiState.requests) { request ->
                        RequestItemCard(
                            request = request,
                            accountType = uiState.accountType,
                            onClick = { onNavigateToRequestDetails(request.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestStatusFilter(
    selectedStatus: RequestStatus?,
    onStatusSelected: (RequestStatus?) -> Unit
) {
    val statuses = listOf(
        null to stringResource(R.string.request_filter_all),
        RequestStatus.PENDING to stringResource(R.string.request_status_submitted),
        RequestStatus.QUOTE_PENDING to stringResource(R.string.request_status_quote_pending),
        RequestStatus.ACCEPTED to stringResource(R.string.request_status_approved),
        RequestStatus.IN_PROGRESS to stringResource(R.string.request_status_in_progress),
        RequestStatus.FULFILLED to stringResource(R.string.request_status_completed),
        RequestStatus.REJECTED to stringResource(R.string.request_status_rejected),
        RequestStatus.CANCELLED to stringResource(R.string.request_status_cancelled),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = MaterialTheme.dimens.spaceXS),
    ) {
        items(statuses, key = { it.first?.name ?: "ALL" }) { (status, label) ->
            FilterChip(
                selected = selectedStatus == status,
                label = label,
                onClick = { onStatusSelected(status) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun RequestItemCard(
    request: Request,
    accountType: AccountType?,
    onClick: () -> Unit
) {
    val d = MaterialTheme.dimens
    val basketItems = request.displayItems()

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "طلب #${request.id.takeLast(5)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral600,
                )
                StatusPill(status = request.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = request.primaryPartyName(accountType),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${basketItems.size} أصناف",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        color = PharmaNeutral100,
                    )
                    Text(
                        text = request.updatedAtLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PriceStatePill(request = request)
                
                Text(
                    text = "عرض التفاصيل >",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RequesterIdentityPreview(request: Request) {
    val d = MaterialTheme.dimens
    val details = listOfNotNull(
        request.pharmacyPhone.takeIf { it.isNotBlank() }?.let {
            Icons.Outlined.Phone to it
        },
        request.pharmacyLocation.takeIf { it.isNotBlank() }?.let {
            Icons.Outlined.LocationOn to it
        },
    )

    details.forEach { (icon, value) ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IncomingBasketPreviewRow(item: RequestItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = item.medicineName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${item.quantity} ${item.unit}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusPill(status: RequestStatus) {
    Surface(
        shape = CircleShape,
        color = statusContainerColor(status),
        contentColor = statusContentColor(status),
    ) {
        Text(
            text = statusLabel(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PriceStatePill(request: Request) {
    val isPriced = request.totalPrice > 0.0
    Surface(
        shape = CircleShape,
        color = if (isPriced) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isPriced) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = if (isPriced) {
                    "${request.totalPrice.toLong()} \u0644.\u0633"
                } else {
                    "\u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0627\u0644\u062A\u0633\u0639\u064A\u0631"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun UrgentPill() {
    Surface(
        shape = CircleShape,
        color = PremiumUrgent.copy(alpha = 0.12f),
        contentColor = PremiumUrgent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = stringResource(R.string.request_priority_urgent),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.request_priority_urgent),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun statusContainerColor(status: RequestStatus) = when (status) {
    RequestStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
    RequestStatus.QUOTE_PENDING -> MaterialTheme.colorScheme.secondaryContainer
    RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
    RequestStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
    RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primaryContainer
    RequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
    RequestStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
    RequestStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun statusContentColor(status: RequestStatus) = when (status) {
    RequestStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
    RequestStatus.QUOTE_PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
    RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
    RequestStatus.ACCEPTED -> MaterialTheme.colorScheme.onPrimaryContainer
    RequestStatus.FULFILLED -> MaterialTheme.colorScheme.onPrimaryContainer
    RequestStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
    RequestStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    RequestStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun statusLabel(status: RequestStatus): String = when (status) {
    RequestStatus.PENDING -> stringResource(R.string.request_status_submitted)
    RequestStatus.QUOTE_PENDING -> stringResource(R.string.request_status_quote_pending)
    RequestStatus.ACCEPTED -> stringResource(R.string.request_status_approved)
    RequestStatus.IN_PROGRESS -> stringResource(R.string.request_status_in_progress)
    RequestStatus.FULFILLED -> stringResource(R.string.request_status_completed)
    RequestStatus.REJECTED -> stringResource(R.string.request_status_rejected)
    RequestStatus.CANCELLED -> stringResource(R.string.request_status_cancelled)
    RequestStatus.DRAFT -> stringResource(R.string.request_status_draft)
}

private fun Request.displayItems(): List<RequestItem> =
    items.ifEmpty {
        listOf(
            RequestItem(
                lineNo = 1,
                medicineId = medicineId.orEmpty(),
                medicineName = medicineName,
                medicineSubtitle = medicineSubtitle,
                quantity = quantity,
                unit = unit,
            ),
        )
    }

private fun Request.primaryPartyName(accountType: AccountType?): String =
    if (accountType == AccountType.WAREHOUSE) {
        pharmacyName.ifBlank { "\u0635\u064A\u062F\u0644\u064A\u0629" }
    } else {
        warehouseName.ifBlank { supplierName.ifBlank { medicineName } }
    }

@Composable
private fun Modifier.shimmerEffect(shape: RoundedCornerShape = RoundedCornerShape(MaterialTheme.dimens.radiusM)): Modifier {
    val shimmerColors = listOf(
        LightGray.copy(alpha = 0.6f),
        LightGray.copy(alpha = 0.2f),
        LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerTranslate",
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(10f, 10f),
        end = Offset(translateAnimation.value, translateAnimation.value),
    )

    return this.background(brush)
}

@Composable
private fun RequestItemCardPlaceholder() {
    val d = MaterialTheme.dimens
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM)
        ) {
            Spacer(modifier = Modifier.width(200.dp).height(20.dp).shimmerEffect())
            Spacer(modifier = Modifier.height(d.spaceS))
            Spacer(modifier = Modifier.width(150.dp).height(16.dp).shimmerEffect())
            Spacer(modifier = Modifier.height(d.spaceS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(100.dp).height(16.dp).shimmerEffect())
                Spacer(modifier = Modifier.width(80.dp).height(16.dp).shimmerEffect())
            }
            Spacer(modifier = Modifier.height(d.spaceS))
            Spacer(modifier = Modifier.width(120.dp).height(16.dp).shimmerEffect())
        }
    }
}
