package com.pharmalink.feature.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.StockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

@Composable
fun WarehouseDashboardScreen(
    warehouseId: String,
    warehouseName: String,
    onManageInventory: () -> Unit,
    onAddProduct: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarehouseDashboardViewModel = hiltViewModel(),
) {
    val hasLinkedWarehouse = warehouseId.isNotBlank()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(warehouseId) {
        viewModel.load(warehouseId)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
            contentPadding = PaddingValues(MaterialTheme.dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
        ) {
            item {
                WarehouseHeroCard(
                    warehouseName = warehouseName,
                    hasLinkedWarehouse = hasLinkedWarehouse,
                )
            }

            if (!hasLinkedWarehouse) {
                item {
                    WarehouseLinkBlockingCard(onOpenProfile = onOpenProfile)
                }
            }

            if (hasLinkedWarehouse) {
                item {
                    DashboardStatsGrid(
                        stats = uiState.stats,
                        isLoading = uiState.isLoading,
                    )
                }
            }

            item {
                Text(
                    text = "\u0625\u062f\u0627\u0631\u0629 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
                    ) {
                        WarehouseDashboardActionCard(
                            title = "\u0645\u0639\u0631\u0636 \u0645\u0646\u062a\u062c\u0627\u062a\u064a",
                            subtitle = "\u0627\u0644\u0643\u0645\u064a\u0627\u062a \u0648\u0627\u0644\u0638\u0647\u0648\u0631",
                            icon = Icons.Outlined.Inventory2,
                            enabled = hasLinkedWarehouse,
                            onClick = onManageInventory,
                            modifier = Modifier.weight(1f),
                        )
                        WarehouseDashboardActionCard(
                            title = "\u0625\u0636\u0627\u0641\u0629 \u0645\u0646\u062a\u062c",
                            subtitle = "\u0635\u0648\u0631\u0629 \u0648\u0633\u0639\u0631 \u0648\u0643\u0645\u064a\u0629",
                            icon = Icons.Outlined.AddBox,
                            enabled = hasLinkedWarehouse,
                            onClick = onAddProduct,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
                    ) {
                        WarehouseDashboardActionCard(
                            title = "\u0637\u0644\u0628\u0627\u062a \u0627\u0644\u0635\u064a\u062f\u0644\u064a\u0627\u062a",
                            subtitle = "\u0627\u0644\u0648\u0627\u0631\u062f\u0629",
                            icon = Icons.Outlined.Assignment,
                            onClick = onOpenRequests,
                            modifier = Modifier.weight(1f),
                        )
                        WarehouseDashboardActionCard(
                            title = "\u0627\u0644\u0625\u0634\u0639\u0627\u0631\u0627\u062a",
                            subtitle = "\u062a\u0646\u0628\u064a\u0647\u0627\u062a \u0648\u062a\u062d\u062f\u064a\u062b\u0627\u062a",
                            icon = Icons.Outlined.NotificationsNone,
                            onClick = onOpenNotifications,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    WarehouseDashboardActionCard(
                        title = "\u0645\u0648\u0642\u0639 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639",
                        subtitle = "\u0627\u0644\u0639\u0646\u0648\u0627\u0646 \u0648\u0627\u0644\u062e\u0631\u064a\u0637\u0629",
                        icon = Icons.Outlined.LocationOn,
                        onClick = onOpenProfile,
                    )
                    WarehouseDashboardActionCard(
                        title = "\u0627\u0644\u0645\u0644\u0641 \u0627\u0644\u0634\u062e\u0635\u064a",
                        subtitle = "\u0627\u0644\u062d\u0633\u0627\u0628 \u0648\u0627\u0644\u0628\u064a\u0627\u0646\u0627\u062a",
                        icon = Icons.Outlined.Person,
                        onClick = onOpenProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseHeroCard(
    warehouseName: String,
    hasLinkedWarehouse: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            modifier = Modifier.size(d.iconL),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "\u0644\u0648\u062d\u0629 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = warehouseName.ifBlank { "\u062d\u0633\u0627\u0628 \u0645\u0633\u062a\u0648\u062f\u0639" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PharmaStatusChip(
                    label = if (hasLinkedWarehouse) "\u0645\u0631\u062a\u0628\u0637" else "\u063a\u064a\u0631 \u0645\u0631\u062a\u0628\u0637",
                    tone = if (hasLinkedWarehouse) StatusTone.Success else StatusTone.Urgent,
                )
            }
            Text(
                text = if (hasLinkedWarehouse) {
                    "\u0623\u062f\u0631 \u0645\u0639\u0631\u0636 \u0645\u0646\u062a\u062c\u0627\u062a\u0643\u060c \u0627\u0633\u062a\u0642\u0628\u0644 \u0637\u0644\u0628\u0627\u062a \u0627\u0644\u0635\u064a\u062f\u0644\u064a\u0627\u062a\u060c \u0648\u062a\u0627\u0628\u0639 \u062d\u0627\u0644\u0629 \u062d\u0633\u0627\u0628\u0643 \u0645\u0646 \u0645\u0643\u0627\u0646 \u0648\u0627\u062d\u062f."
                } else {
                    "\u064a\u062c\u0628 \u0631\u0628\u0637 \u062d\u0633\u0627\u0628 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639 \u0628\u0645\u0646\u0634\u0623\u0629 \u0642\u0628\u0644 \u0625\u062f\u0627\u0631\u0629 \u0627\u0644\u0645\u0646\u062a\u062c\u0627\u062a."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DashboardStatsGrid(
    stats: WarehouseDashboardStats?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val loadingValue = "\u2026"
    val items = listOf(
        "\u0627\u0644\u0645\u0646\u062a\u062c\u0627\u062a" to (stats?.productsCount?.toString() ?: loadingValue),
        "\u0627\u0644\u0648\u0627\u0631\u062f\u0629" to (stats?.incomingRequestsCount?.toString() ?: loadingValue),
        "\u0628\u0627\u0646\u062a\u0638\u0627\u0631 \u0645\u0648\u0627\u0641\u0642\u0629 \u0627\u0644\u0635\u064a\u062f\u0644\u064a\u0629" to (stats?.quotePendingCount?.toString() ?: loadingValue),
        "\u0645\u062e\u0632\u0648\u0646 \u0645\u0646\u062e\u0641\u0636" to (stats?.lowStockCount?.toString() ?: loadingValue),
        "\u0645\u062e\u0641\u064a/\u063a\u064a\u0631 \u0646\u0634\u0637" to (stats?.hiddenInactiveCount?.toString() ?: loadingValue),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                rowItems.forEach { (label, value) ->
                    DashboardStatCard(
                        label = label,
                        value = if (stats == null && !isLoading) "\u2014" else value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

data class WarehouseDashboardUiState(
    val isLoading: Boolean = false,
    val stats: WarehouseDashboardStats? = null,
)

data class WarehouseDashboardStats(
    val productsCount: Int,
    val incomingRequestsCount: Int,
    val quotePendingCount: Int,
    val lowStockCount: Int,
    val hiddenInactiveCount: Int,
)

@HiltViewModel
class WarehouseDashboardViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WarehouseDashboardUiState())
    val uiState: StateFlow<WarehouseDashboardUiState> = _uiState.asStateFlow()
    private var requestJob: Job? = null

    fun load(warehouseId: String) {
        requestJob?.cancel()
        if (warehouseId.isBlank()) {
            _uiState.value = WarehouseDashboardUiState()
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            pharmaRepository.getWarehouseInventory(warehouseId).onSuccess { inventory ->
                _uiState.update { current ->
                    val currentStats = current.stats
                    current.copy(
                        isLoading = false,
                        stats = WarehouseDashboardStats(
                            productsCount = inventory.size,
                            incomingRequestsCount = currentStats?.incomingRequestsCount ?: 0,
                            quotePendingCount = currentStats?.quotePendingCount ?: 0,
                            lowStockCount = inventory.count { item -> item.stockStatus == StockStatus.LOW_STOCK },
                            hiddenInactiveCount = inventory.count { item -> !item.isVisible || !item.isActive },
                        ),
                    )
                }
            }.onFailure {
                _uiState.update { current -> current.copy(isLoading = false) }
            }
        }

        requestJob = viewModelScope.launch {
            pharmaRepository.observeIncomingRequestsForWarehouse(warehouseId).collect { requests ->
                val incomingCount = requests.count { request ->
                    request.status == RequestStatus.PENDING ||
                        request.status == RequestStatus.QUOTE_PENDING ||
                        request.status == RequestStatus.ACCEPTED ||
                        request.status == RequestStatus.IN_PROGRESS
                }
                val quotePendingCount = requests.count { request ->
                    request.status == RequestStatus.QUOTE_PENDING
                }
                _uiState.update { current ->
                    val currentStats = current.stats
                    current.copy(
                        stats = if (currentStats == null) {
                            WarehouseDashboardStats(
                                productsCount = 0,
                                incomingRequestsCount = incomingCount,
                                quotePendingCount = quotePendingCount,
                                lowStockCount = 0,
                                hiddenInactiveCount = 0,
                            )
                        } else {
                            currentStats.copy(
                                incomingRequestsCount = incomingCount,
                                quotePendingCount = quotePendingCount,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseLinkBlockingCard(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(d.iconM),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "\u062d\u0633\u0627\u0628 \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639 \u063a\u064a\u0631 \u0645\u0631\u062a\u0628\u0637 \u0628\u0645\u0646\u0634\u0623\u0629",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "\u0625\u062f\u0627\u0631\u0629 \u0627\u0644\u0645\u062e\u0632\u0648\u0646 \u0648\u0625\u0636\u0627\u0641\u0629 \u0627\u0644\u0645\u0646\u062a\u062c\u0627\u062a \u0645\u062a\u0648\u0642\u0641\u0629 \u062d\u062a\u0649 \u064a\u062a\u0645 \u0631\u0628\u0637 \u0627\u0644\u062d\u0633\u0627\u0628.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                onClick = onOpenProfile,
            ) {
                Text(
                    text = "\u0627\u0644\u0645\u0644\u0641",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                )
            }
        }
    }
}

@Composable
private fun WarehouseDashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val d = MaterialTheme.dimens
    val contentAlpha = if (enabled) 1f else 0.48f

    Card(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.Start,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = contentAlpha),
                contentColor = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(d.iconM),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
