package com.pharmalink.feature.admin.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pharmalink.designsystem.theme.StatusActive
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.feature.admin.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarButton

@Composable
fun AdminWarehousesScreen(
    onNavigateToCreateWarehouse: () -> Unit,
    onNavigateToWarehouseDetail: (String) -> Unit,
    onNavigateToInventory: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onShowAdminMenu: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminWarehousesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminWarehousesEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            AdminWarehousesEffect.ShowAdminMenu -> onShowAdminMenu()
            is AdminWarehousesEffect.NavigateToWarehouseDetail -> {
                onNavigateToWarehouseDetail(effect.warehouseId)
            }
            is AdminWarehousesEffect.NavigateToInventoryManagement -> {
                onNavigateToInventory(effect.warehouseId)
            }
        }
    }

    AdminWarehousesContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToCreateWarehouse = onNavigateToCreateWarehouse,
        onNavigateToProfile = onNavigateToProfile,
        profileImageUrl = profileImageUrl,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminWarehousesContent(
    state: AdminWarehousesUiState,
    onAction: (AdminWarehousesAction) -> Unit,
    onNavigateToCreateWarehouse: () -> Unit,
    onNavigateToProfile: () -> Unit,
    profileImageUrl: String? = null,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.admin_warehouses_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(AdminWarehousesAction.OnMenuClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.admin_menu_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        AdminProfileAvatarButton(
                            profileImageUrl = profileImageUrl,
                            contentDescription = stringResource(R.string.admin_profile_cd),
                            onClick = onNavigateToProfile,
                        )
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateWarehouse,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.admin_add_warehouse_cd),
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminWarehousesAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.warehouses.isEmpty() -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        PharmaSkeletonLine(heightDp = 60f)
        Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
            PharmaSkeletonLine(heightDp = 100f, modifier = Modifier.weight(1f))
            PharmaSkeletonLine(heightDp = 100f, modifier = Modifier.weight(1f))
        }
        repeat(4) {
            PharmaSkeletonLine(heightDp = 140f)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.admin_warehouses_error),
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = stringResource(R.string.admin_warehouses_retry),
            onAction = onRetry,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.admin_warehouses_empty),
            subtitle = stringResource(R.string.admin_warehouses_empty_subtitle),
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    state: AdminWarehousesUiState,
    onAction: (AdminWarehousesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Search and Sort Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                TextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(AdminWarehousesAction.OnSearchQueryChanged(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.admin_warehouses_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true,
                )
                
                // Sort Dropdown
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { sortExpanded = true },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.admin_sort_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("الاسم") },
                            onClick = {
                                onAction(AdminWarehousesAction.OnSortByChanged(WarehouseSortBy.NAME))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("الموقع") },
                            onClick = {
                                onAction(AdminWarehousesAction.OnSortByChanged(WarehouseSortBy.LOCATION))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تاريخ الإضافة") },
                            onClick = {
                                onAction(AdminWarehousesAction.OnSortByChanged(WarehouseSortBy.DATE_ADDED))
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Warehouse Cards
        items(
            items = state.warehouses,
            key = { it.id },
        ) { warehouse ->
            WarehouseCard(
                warehouse = warehouse,
                onCardClick = { onAction(AdminWarehousesAction.OnWarehouseClicked(warehouse.id)) },
                onManageInventoryClick = { onAction(AdminWarehousesAction.OnManageInventoryClicked(warehouse.id)) },
            )
        }
    }
}

@Composable
private fun WarehouseCard(
    warehouse: WarehouseItemModel,
    onCardClick: () -> Unit,
    onManageInventoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onCardClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    
                    Column {
                        Text(
                            text = warehouse.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        
                        Text(
                            text = warehouse.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (warehouse.isActive) {
                        StatusActive.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
                ) {
                    Text(
                        text = stringResource(if (warehouse.isActive) R.string.admin_status_active else R.string.admin_status_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (warehouse.isActive) {
                            StatusActive
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(
                            horizontal = d.spaceS,
                            vertical = d.spaceXS,
                        ),
                    )
                }
            }
            
            // Temperature and Inventory
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceL),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AcUnit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = warehouse.temperature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.admin_inventory_items, warehouse.inventoryCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
            
            // Action Button
            PharmaButton(
                text = stringResource(R.string.admin_warehouses_manage_inventory),
                onClick = onManageInventoryClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminWarehousesScreen() {
    PharmaTheme {
        AdminWarehousesContent(
            state = AdminWarehousesUiState(
                warehouses = listOf(
                    WarehouseItemModel(
                        id = "1",
                        name = "مستودع المركز الرئيسي",
                        address = "المنطقة الصناعية، الرياض",
                        isActive = true,
                        temperature = "2-8°C",
                        inventoryCount = 522,
                    ),
                    WarehouseItemModel(
                        id = "2",
                        name = "مستودع التبريد الشمالي",
                        address = "طريق الملك فهد، جدة",
                        isActive = false,
                        temperature = "عادي",
                        inventoryCount = 54,
                    ),
                ),
                // activeShipments removed - not available
            ),
            onAction = {},
            onNavigateToCreateWarehouse = {},
            onNavigateToProfile = {},
            profileImageUrl = null,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
