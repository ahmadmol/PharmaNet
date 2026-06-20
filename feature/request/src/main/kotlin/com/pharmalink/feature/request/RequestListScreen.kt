package com.pharmalink.feature.request

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestItem
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus

@Composable
fun RequestListScreen(
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToRequestDetails: (String) -> Unit,
    viewModel: RequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RequestListContent(
        uiState = uiState,
        onNavigateToCreateRequest = onNavigateToCreateRequest,
        onNavigateToRequestDetails = onNavigateToRequestDetails,
        onRefresh = viewModel::refreshRequests,
        onFilterByStatus = viewModel::filterByStatus
    )
}

@Composable
private fun RequestListContent(
    uiState: RequestListUiState,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToRequestDetails: (String) -> Unit,
    onRefresh: () -> Unit,
    onFilterByStatus: (RequestStatus?) -> Unit
) {
    val isPharmacyUser = uiState.accountType == AccountType.PHARMACY
    val d = MaterialTheme.dimens
    val headerTitle = if (uiState.accountType == AccountType.WAREHOUSE) {
        stringResource(R.string.request_list_incoming_title)
    } else {
        stringResource(R.string.request_list_title)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Brand Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceM, vertical = d.spaceS),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { /* Handle back or internal navigation */ },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = PharmaBlue500
                    )
                }
                Text(
                    text = "فارمانيت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PharmaBlue500
                )
            }
            HorizontalDivider(color = PharmaNeutral100, thickness = 1.dp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = d.spaceXL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.spaceL, vertical = d.spaceM),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM)
                ) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PharmaNeutral900,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Status Filter
                    RequestStatusFilter(
                        selectedStatus = uiState.selectedStatus,
                        onStatusSelected = onFilterByStatus
                    )
                }
            }

            // Error State
            uiState.errorMessage?.let { error ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(d.spaceXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(d.spaceM))
                        Button(onClick = onRefresh) {
                            Text(stringResource(R.string.request_retry_loading))
                        }
                    }
                }
            }

            // Content
            when {
                uiState.isLoading && uiState.requests.isEmpty() -> {
                    items(5) {
                        Box(modifier = Modifier.padding(horizontal = d.spaceL)) {
                            RequestItemCardPlaceholder()
                        }
                    }
                }

                uiState.requests.isEmpty() && !uiState.isLoading && uiState.errorMessage == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(d.spaceS),
                                modifier = Modifier.padding(d.spaceL)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = PharmaNeutral400
                                )
                                Text(
                                    text = stringResource(R.string.request_list_empty),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PharmaNeutral900
                                )
                                Text(
                                    text = if (isPharmacyUser) {
                                        stringResource(R.string.request_empty_subtitle)
                                    } else {
                                        stringResource(R.string.request_empty_incoming_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PharmaNeutral600,
                                    textAlign = TextAlign.Center
                                )

                                if (isPharmacyUser) {
                                    Spacer(modifier = Modifier.height(d.spaceM))
                                    Button(onClick = onNavigateToCreateRequest) {
                                        Text(stringResource(R.string.request_create_first))
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (uiState.isLoading && uiState.requests.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(d.spaceM),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    items(uiState.requests, key = { it.id }) { request ->
                        Box(modifier = Modifier.padding(horizontal = d.spaceL)) {
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
        contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.spaceXS),
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
    Surface(
        onClick = onClick,
        modifier = Modifier,
        shape = CircleShape,
        color = if (selected) {
            PharmaBlue500.copy(alpha = 0.1f)
        } else {
            PharmaNeutral100.copy(alpha = 0.5f)
        },
        border = if (selected) {
            BorderStroke(1.dp, PharmaBlue500.copy(alpha = 0.2f))
        } else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                PharmaBlue500
            } else {
                PharmaNeutral600
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
    val isWarehouse = accountType == AccountType.WAREHOUSE

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        shadowElevation = 0.5.dp,
        border = BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            // Pharmacy Name and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(status = request.status)
                Text(
                    text = if (isWarehouse) request.pharmacyName.ifBlank { "صيدلية" } else request.warehouseName.ifBlank { "مستودع" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900,
                )
            }

            // Contact Info
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isWarehouse) {
                    ContactRow(icon = Icons.Outlined.Phone, text = request.pharmacyPhone.ifBlank { "غير متوفر" })
                    ContactRow(icon = Icons.Outlined.LocationOn, text = request.pharmacyLocation.ifBlank { "غير متوفر" })
                }
            }

            // Basket Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusL),
                color = PharmaBlue50.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier.padding(d.spaceM),
                    verticalArrangement = Arrangement.spacedBy(d.spaceS),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "عناصر السلة: ${basketItems.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PharmaBlue500,
                        textAlign = TextAlign.End
                    )
                    
                    basketItems.firstOrNull()?.let { firstItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${firstItem.medicineName} - ${firstItem.quantity} ${firstItem.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PharmaNeutral900,
                                textAlign = TextAlign.End
                            )
                            Spacer(Modifier.width(d.spaceXS))
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PharmaBlue500
                            )
                        }
                    }
                    
                    if (basketItems.size > 1) {
                        Text(
                            text = "+${basketItems.size - 1} عناصر أخرى",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PharmaBlue500,
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Bottom Actions (Price & Priority)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (request.priority == RequestPriority.URGENT) {
                    UrgentPill()
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                
                PriceStatePill(request = request)
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXS)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = PharmaNeutral600
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = PharmaNeutral400
        )
    }
}

@Composable
private fun StatusPill(status: RequestStatus) {
    Surface(
        shape = CircleShape,
        color = statusContainerColor(status).copy(alpha = 0.12f),
        contentColor = statusContentColor(status),
    ) {
        Text(
            text = statusLabel(status),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PriceStatePill(request: Request) {
    val isPriced = request.totalPrice > 0.0
    Surface(
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusM),
        color = if (isPriced) {
            PharmaSuccess.copy(alpha = 0.1f)
        } else {
            PharmaBlue500.copy(alpha = 0.1f)
        },
        contentColor = if (isPriced) {
            PharmaSuccess
        } else {
            PharmaBlue500
        },
    ) {
        Text(
            text = if (isPriced) {
                "${request.totalPrice.toLong()} \u0644.\u0633"
            } else {
                "\u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0627\u0644\u062A\u0633\u0639\u064A\u0631"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UrgentPill() {
    Surface(
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusM),
        color = PremiumUrgent.copy(alpha = 0.1f),
        contentColor = PremiumUrgent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.request_priority_urgent),
                style = MaterialTheme.typography.labelLarge,
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

@Preview(showBackground = true)
@Composable
private fun RequestListPreview() {
    com.pharmalink.designsystem.theme.PharmaTheme {
        RequestListContent(
            uiState = RequestListUiState(
                requests = listOf(
                    Request(
                        id = "1",
                        pharmacyId = "p1",
                        pharmacyName = "صيدلية الصحة",
                        medicineName = "بانادول",
                        quantity = 10,
                        unit = "علبة",
                        status = RequestStatus.PENDING,
                        updatedAtLabel = "منذ ساعة",
                        priority = RequestPriority.NORMAL,
                        notes = "",
                        warehouseId = "w1",
                        warehouseName = "مستودع الأمل",
                        supplierName = "مستودع الأمل",
                        createdAtLabel = "منذ ساعة"
                    ),
                    Request(
                        id = "2",
                        pharmacyId = "p1",
                        pharmacyName = "صيدلية الأمل",
                        medicineName = "أسبيرين",
                        quantity = 5,
                        unit = "علبة",
                        status = RequestStatus.QUOTE_PENDING,
                        updatedAtLabel = "منذ ساعتين",
                        priority = RequestPriority.URGENT,
                        notes = "",
                        warehouseId = "w1",
                        warehouseName = "مستودع الأمل",
                        supplierName = "مستودع الأمل",
                        createdAtLabel = "منذ ساعتين"
                    )
                ),
                accountType = AccountType.WAREHOUSE
            ),
            onNavigateToCreateRequest = {},
            onNavigateToRequestDetails = {},
            onRefresh = {},
            onFilterByStatus = {}
        )
    }
}
