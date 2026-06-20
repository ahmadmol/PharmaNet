package com.pharmalink.feature.warehouses

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens

private data class FeaturedFilter(
    val labelRes: Int,
    val predicate: (FeaturedWarehouseItem) -> Boolean,
)

@Composable
fun FeaturedWarehousesScreen(
    onBack: () -> Unit,
    onWarehouseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeaturedWarehousesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PharmaScreenScaffold(
            title = stringResource(R.string.featured_warehouses_screen_title),
            onBack = onBack,
            navigationContentDescription = stringResource(R.string.featured_warehouses_back_cd),
            modifier = modifier,
        ) {
            FeaturedWarehousesContent(
                uiState = uiState,
                onWarehouseClick = onWarehouseClick,
                onRetry = viewModel::refreshFeaturedWarehouses,
            )
        }
    }
}

@Composable
private fun FeaturedWarehousesContent(
    uiState: FeaturedWarehousesUiState,
    onWarehouseClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val d = MaterialTheme.dimens

    val filters = remember {
        listOf(
            FeaturedFilter(R.string.featured_filter_all) { true },
            FeaturedFilter(R.string.featured_filter_fast_delivery) {
                val delivery = it.deliveryLabel
                delivery.contains("ساعت") || delivery.contains("نفس اليوم") || delivery.contains("سريع")
            },
            FeaturedFilter(R.string.featured_filter_high_stock) { it.inStockPercent >= 85 },
            FeaturedFilter(R.string.featured_filter_cold_chain) { it.supportsColdChain },
        )
    }

    var selectedFilterRes by rememberSaveable { mutableStateOf(R.string.featured_filter_all) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectedFilter = remember(selectedFilterRes, filters) {
        filters.firstOrNull { it.labelRes == selectedFilterRes } ?: filters.first()
    }

    val filteredWarehouses = remember(selectedFilter, uiState.warehouses, searchQuery) {
        uiState.warehouses
            .filter(selectedFilter.predicate)
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true)
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
        contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceM),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item { FeaturedHeroCard(totalCount = uiState.warehouses.size) }

        item {
            SearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                items(filters, key = { it.labelRes }) { filter ->
                    val selected = filter.labelRes == selectedFilter.labelRes
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilterRes = filter.labelRes },
                        label = {
                            Text(
                                text = stringResource(filter.labelRes),
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                    )
                }
            }
        }

        item { SectionResultHeader(count = filteredWarehouses.size) }

        if (uiState.isLoading && uiState.warehouses.isEmpty()) {
            item { FeaturedLoadingState() }
            return@LazyColumn
        }

        if (uiState.errorMessage != null && uiState.warehouses.isEmpty()) {
            item {
                FeaturedErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                )
            }
            return@LazyColumn
        }

        if (filteredWarehouses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(d.radiusXXL),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = d.cardElevation,
                ) {
                    Text(
                        text = if (uiState.warehouses.isEmpty()) {
                            stringResource(R.string.featured_empty_source_state)
                        } else {
                            stringResource(R.string.featured_empty_state)
                        },
                        modifier = Modifier.padding(d.spaceL),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(filteredWarehouses, key = { it.id }) { warehouse ->
                FeaturedWarehouseCard(
                    warehouse = warehouse,
                    onClick = { onWarehouseClick(warehouse.id) },
                )
            }
        }

        if (uiState.isLoading && uiState.warehouses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceS),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroCard(totalCount: Int) {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = PharmaGradients.primaryDiagonal)
                .padding(d.spaceL),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = stringResource(R.string.featured_hero_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                    )
                }
                Text(
                    text = stringResource(R.string.featured_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.featured_hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.94f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(d.iconS),
                    )
                    Text(
                        text = stringResource(R.string.featured_hero_kpi, totalCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(d.iconS),
            )
            Spacer(Modifier.width(d.spaceS))
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.featured_search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun SectionResultHeader(count: Int) {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(d.spaceXXS)) {
            Text(
                text = stringResource(R.string.featured_results_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.featured_results_count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = CircleShape,
            color = PharmaBlue500.copy(alpha = 0.12f),
            contentColor = PharmaBlue700,
        ) {
            Text(
                text = stringResource(R.string.featured_sort_recommended),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
            )
        }
    }
}

@Composable
private fun FeaturedWarehouseCard(
    warehouse: FeaturedWarehouseItem,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val stockColor = if (warehouse.inStockPercent >= 80) PharmaSuccess else PremiumUrgent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(d.radiusL),
                        color = PharmaBlue500.copy(alpha = 0.12f),
                        contentColor = PharmaBlue700,
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
                        verticalArrangement = Arrangement.spacedBy(d.spaceXXS),
                    ) {
                        Text(
                            text = warehouse.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(d.spaceXXS),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(d.iconXS),
                            )
                            Text(
                                text = warehouse.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                PriorityPill(label = stringResource(warehouse.priorityRes))
            }

            MetricTile(
                title = stringResource(R.string.featured_metric_delivery),
                value = warehouse.deliveryLabel,
                icon = Icons.Outlined.Bolt,
            )

            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.spaceM, vertical = d.spaceS),
                    verticalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = stockColor,
                                modifier = Modifier.size(d.iconS),
                            )
                            Text(
                                text = stringResource(R.string.featured_metric_stock),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.featured_metric_stock_value, warehouse.inStockPercent),
                            style = MaterialTheme.typography.titleSmall,
                            color = stockColor,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (warehouse.inStockPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = stockColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    )
                }
            }

            if (warehouse.supportsColdChain) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.featured_chip_cold_chain),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedLoadingState() {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.featured_loading_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeaturedErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.featured_retry_loading))
            }
        }
    }
}

@Composable
private fun FeaturedInlineErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceM, vertical = d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.featured_retry_loading))
            }
        }
    }
}

@Composable
private fun PriorityPill(label: String) {
    val d = MaterialTheme.dimens

    Surface(
        shape = CircleShape,
        color = PremiumUrgent.copy(alpha = 0.14f),
        contentColor = PremiumUrgent,
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
private fun MetricTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val d = MaterialTheme.dimens

    Surface(
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceM, vertical = d.spaceS),
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PharmaBlue700,
                modifier = Modifier.size(d.iconS),
            )
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXXS)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeaturedWarehousesPreview() {
    com.pharmalink.designsystem.theme.PharmaTheme {
        FeaturedWarehousesContent(
            uiState = FeaturedWarehousesUiState(
                warehouses = listOf(
                    FeaturedWarehouseItem(
                        id = "1",
                        name = "مستودع الأمل للأدوية",
                        location = "الرياض، حي السلي",
                        deliveryLabel = "توصيل خلال ساعتين",
                        deliveryType = FeaturedDeliveryType.FAST,
                        inStockPercent = 95,
                        priorityRes = R.string.featured_priority_premium,
                        supportsColdChain = true
                    ),
                    FeaturedWarehouseItem(
                        id = "2",
                        name = "مستودع الشفاء المركزي",
                        location = "جدة، المنطقة الصناعية",
                        deliveryLabel = "توصيل نفس اليوم",
                        deliveryType = FeaturedDeliveryType.STANDARD,
                        inStockPercent = 82,
                        priorityRes = R.string.featured_priority_reliable,
                        supportsColdChain = false
                    )
                )
            ),
            onWarehouseClick = {},
            onRetry = {}
        )
    }
}
