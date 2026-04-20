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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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


@Composable
fun RequestListScreen(
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToRequestDetails: (String) -> Unit,
    viewModel: RequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPharmacyUser = uiState.accountType == AccountType.PHARMACY
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (isPharmacyUser) {
                Button(onClick = onNavigateToCreateRequest) {
                    Text(stringResource(R.string.request_create_new))
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
        RequestStatus.ACCEPTED to stringResource(R.string.request_status_approved),
        RequestStatus.IN_PROGRESS to stringResource(R.string.request_status_under_review),
        RequestStatus.FULFILLED to stringResource(R.string.request_status_completed),
        RequestStatus.REJECTED to stringResource(R.string.request_status_rejected),
        RequestStatus.CANCELLED to stringResource(R.string.request_status_cancelled),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)
    ) {
        statuses.forEach { (status, label) ->
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceM)
        ) {
            Text(
                text = request.medicineName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (request.medicineSubtitle.isNotBlank()) {
                Text(
                    text = request.medicineSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${request.quantity} ${request.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = statusLabel(request.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(request.status)
                )
            }
            
            if (request.warehouseName.isNotBlank()) {
                Text(
                    text = request.warehouseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getStatusColor(status: RequestStatus) = when (status) {
    RequestStatus.PENDING -> MaterialTheme.colorScheme.primary
    RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
    RequestStatus.ACCEPTED -> MaterialTheme.colorScheme.primary
    RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primary
    RequestStatus.REJECTED -> MaterialTheme.colorScheme.error
    RequestStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun statusLabel(status: RequestStatus): String = when (status) {
    RequestStatus.PENDING -> stringResource(R.string.request_status_submitted)
    RequestStatus.ACCEPTED -> stringResource(R.string.request_status_approved)
    RequestStatus.IN_PROGRESS -> stringResource(R.string.request_status_under_review)
    RequestStatus.FULFILLED -> stringResource(R.string.request_status_completed)
    RequestStatus.REJECTED -> stringResource(R.string.request_status_rejected)
    RequestStatus.CANCELLED -> stringResource(R.string.request_status_cancelled)
    RequestStatus.DRAFT -> stringResource(R.string.request_status_draft)
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
