package com.pharmalink.feature.warehouses

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType

private val warehouseCategories = listOf(
    "الكل",
    "أدوية مزمنة",
    "لقاحات",
    "مستلزمات طبية",
)

@Composable
fun WarehousesScreen(
    viewModel: WarehousesViewModel = hiltViewModel(),
    accountType: AccountType? = null,
    onWarehouseClick: (String) -> Unit = {},
    onViewIncomingRequests: () -> Unit = {},
    onAdminCreateFacility: (() -> Unit)? = null,
) {
    val screenTitle = when (accountType) {
        AccountType.WAREHOUSE -> "المستودعات في الشبكة"
        else -> "المستودعات المتاحة"
    }
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf(warehouseCategories.first()) }
    val isWarehouse = accountType == AccountType.WAREHOUSE
    val canCreateRequest = accountType == AccountType.PHARMACY

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            uiState.errorMessage != null -> {
                WarehousesErrorState(
                    error = uiState.errorMessage.orEmpty(),
                    onRetry = viewModel::refreshWarehouses,
                )
            }
            uiState.isLoading && uiState.warehouses.isEmpty() -> {
                WarehousesLoadingState()
            }
            uiState.warehouses.isEmpty() -> {
                WarehousesEmptyState()
            }
            else -> {
                WarehousesContent(
                    warehouses = uiState.warehouses,
                    isRefreshing = uiState.isLoading,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onWarehouseClick = onWarehouseClick,
                    isWarehouse = isWarehouse,
                    canCreateRequest = canCreateRequest,
                    onViewIncomingRequests = onViewIncomingRequests,
                    onAdminCreateFacility = onAdminCreateFacility,
                )
            }
        }
    }
}

@Composable
private fun WarehousesContent(
    warehouses: List<WarehouseItem>,
    isRefreshing: Boolean,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onWarehouseClick: (String) -> Unit,
    isWarehouse: Boolean = false,
    canCreateRequest: Boolean = false,
    onViewIncomingRequests: () -> Unit = {},
    onAdminCreateFacility: (() -> Unit)? = null,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
        contentPadding = PaddingValues(bottom = d.spaceXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            WarehousesSearchBar(
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        if (onAdminCreateFacility != null) {
            item {
                Surface(
                    onClick = onAdminCreateFacility,
                    modifier = Modifier
                        .padding(horizontal = d.spaceL)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(d.spaceL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "إضافة منشأة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        item {
            CategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "المستودعات المتاحة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "عرض الكل",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaBlue500,
                )
            }
        }
        if (isRefreshing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = PharmaBlue500,
                    )
                }
            }
        }
        items(warehouses, key = { it.id }) { item ->
            WarehouseCard(
                item = item,
                modifier = Modifier.padding(horizontal = d.spaceL),
                onWarehouseClick = onWarehouseClick,
                isWarehouse = isWarehouse,
                canCreateRequest = canCreateRequest,
                onViewIncomingRequests = onViewIncomingRequests,
            )
        }
        item {
            WarehousePromo(
                modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceS),
            )
        }
    }
}

@Composable
private fun WarehousesSearchBar(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = PharmaNeutral100,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(d.iconS),
            )
            Text(
                text = "ابحث عن مستودع أو مورد...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(d.iconS),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        items(warehouseCategories) { category ->
            val selected = category == selectedCategory
            Surface(
                shape = CircleShape,
                color = if (selected) PharmaBlue500 else PharmaNeutral100,
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = { onCategorySelected(category) },
                        )
                        .padding(horizontal = d.spaceL, vertical = d.spaceS),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseCard(
    item: WarehouseItem,
    modifier: Modifier = Modifier,
    onWarehouseClick: (String) -> Unit,
    isWarehouse: Boolean = false,
    canCreateRequest: Boolean = false,
    onViewIncomingRequests: () -> Unit = {},
) {
    val actionText = if (isWarehouse) {
        "عرض الطلبات الواردة"
    } else if (canCreateRequest) {
        "اطلب الآن"
    } else {
        null
    }
    val d = MaterialTheme.dimens
    val statusLabel = statusLabel(item.statusType)
    val statusColor = statusColor(item.statusType)
    val fastDelivery = item.estimatedDelivery.isFastDelivery()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isWarehouse) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onWarehouseClick(item.id) },
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(d.radiusL),
                    color = PharmaBlue500.copy(alpha = 0.72f),
                    contentColor = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Warehouse,
                            contentDescription = null,
                            modifier = Modifier.size(d.iconM),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(d.spaceXXS),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(d.iconXS),
                        )
                        Text(
                            text = item.address,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                StatusBadge(label = statusLabel, color = statusColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                WarehouseMetric(
                    label = "سرعة التوصيل",
                    value = item.estimatedDelivery.ifBlank { "غير محدد" },
                    icon = Icons.Outlined.Bolt,
                    valueColor = if (fastDelivery) PharmaSuccess else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                WarehouseMetric(
                    label = "الحد الأدنى",
                    value = "غير محدد",
                    icon = Icons.Outlined.Inventory2,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.supportsColdChain) {
                    WarehouseTag(text = "تبريد خاص", icon = Icons.Outlined.AcUnit)
                }
                if (fastDelivery) {
                    WarehouseTag(text = "شحن سريع", icon = Icons.Outlined.Bolt)
                }
                if (item.stockPercent > 0) {
                    WarehouseTag(text = "المخزون ${item.stockPercent}%", icon = Icons.Outlined.Store)
                }
            }

            if (item.distance.isNotBlank() || item.lastUpdated.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.distance.ifBlank { "المسافة غير محددة" },
                        style = MaterialTheme.typography.labelSmall,
                        color = PharmaNeutral600,
                    )
                    Text(
                        text = item.lastUpdated.ifBlank { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (actionText != null) {
                Button(
                    onClick = {
                        if (isWarehouse) {
                            onViewIncomingRequests()
                        } else {
                            onWarehouseClick(item.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PharmaBlue500,
                        contentColor = Color.White,
                    ),
                    enabled = true,
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun WarehousePromo(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = PharmaBlue500,
        contentColor = Color.White,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier.padding(d.spaceXL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(d.iconM))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "عرض خاص للمستودعات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "اشترك في باقة التوريد الذكي الآن",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f),
                )
            }
        }
    }
}

@Composable
private fun WarehouseMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(d.spaceXXS),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(d.iconXS),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WarehouseTag(text: String, icon: ImageVector) {
    val d = MaterialTheme.dimens

    Surface(
        shape = CircleShape,
        color = PharmaNeutral100,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceXXS),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(icon, contentDescription = null, modifier = Modifier.size(d.iconXS))
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    val d = MaterialTheme.dimens

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
        )
    }
}

@Composable
private fun WarehousesLoadingState() {
    val d = MaterialTheme.dimens

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PharmaBlue500, strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.warehouse_loading_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WarehousesErrorState(error: String, onRetry: () -> Unit) {
    val d = MaterialTheme.dimens

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas)
            .padding(d.spaceL),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(d.radiusXXL),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Column(
                modifier = Modifier.padding(d.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                StitchButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceS),
                ) {
                    Text(stringResource(R.string.warehouse_retry_loading))
                }
            }
        }
    }
}

@Composable
private fun WarehousesEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas)
            .padding(MaterialTheme.dimens.spaceL),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(MaterialTheme.dimens.radiusXXL),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = MaterialTheme.dimens.cardElevation,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
        ) {
            Text(
                text = stringResource(R.string.warehouse_list_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            )
        }
    }
}

@Composable
private fun statusLabel(statusType: StatusType): String = when (statusType) {
    StatusType.AVAILABLE -> "متاح الآن"
    StatusType.LOW_STOCK -> "مخزون منخفض"
    StatusType.CLOSED -> "مغلق حالياً"
}

@Composable
private fun statusColor(statusType: StatusType): Color = when (statusType) {
    StatusType.AVAILABLE -> PharmaSuccess
    StatusType.LOW_STOCK -> PremiumUrgent
    StatusType.CLOSED -> PharmaNeutral600
}

private fun String.isFastDelivery(): Boolean {
    if (isBlank()) return false
    return contains("ساع") || contains("24") || contains("اليوم")
}

@Preview(showBackground = true)
@Composable
fun WarehousesScreenPreview() {
    StitchTheme {
        WarehousesContent(
            warehouses = listOf(
                WarehouseItem(
                    id = "1",
                    name = "مستودع دمشق المركزي",
                    address = "دمشق، المنطقة البركة",
                    status = "متاح",
                    statusType = StatusType.AVAILABLE,
                    supportsColdChain = true,
                    stockPercent = 88,
                    distance = "5 كم",
                    estimatedDelivery = "2 ساعة",
                    lastUpdated = "منذ قليل",
                ),
            ),
            isRefreshing = false,
            selectedCategory = "الكل",
            onCategorySelected = {},
            onWarehouseClick = {},
        )
    }
}
