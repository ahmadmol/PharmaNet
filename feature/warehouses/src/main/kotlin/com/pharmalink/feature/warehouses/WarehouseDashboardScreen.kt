package com.pharmalink.feature.warehouses


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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDashboardScreen(
    modifier: Modifier = Modifier,
    warehouseId: String,
    warehouseName: String,
    onManageInventory: () -> Unit,
    onOpenInventoryFiltered: (filter: String?) -> Unit,
    onAddProduct: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: () -> Unit = {},

    viewModel: WarehouseDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(warehouseId) {
        viewModel.load(warehouseId)
    }

    WarehouseDashboardContent(
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        uiState = uiState,
        onManageInventory = onManageInventory,
        onOpenInventoryFiltered = onOpenInventoryFiltered,
        onAddProduct = onAddProduct,
        onOpenRequests = onOpenRequests,
        onOpenNotifications = onOpenNotifications,
        onOpenProfile = onOpenProfile,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseDashboardContent(
    warehouseId: String,
    warehouseName: String,
    uiState: WarehouseDashboardUiState,
    onManageInventory: () -> Unit,
    onOpenInventoryFiltered: (filter: String?) -> Unit,
    onAddProduct: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.White,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "مستودعي",
                                style = MaterialTheme.typography.titleLarge,
                                color = PharmaBlue900,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = PharmaNeutral600,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.White,
                        ),
                    )
                    HorizontalDivider(color = PharmaNeutral100)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(d.spaceL),
                verticalArrangement = Arrangement.spacedBy(d.spaceL),
            ) {
                item {
                    WarehouseHeroCard(
                        warehouseName = warehouseName,
                        hasLinkedWarehouse = true,
                    )
                }

                item {
                    Text(
                        text = "إحصائيات المستودع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
                item {
                    DashboardStatsGrid(
                        stats = uiState.stats,
                        isLoading = uiState.isLoading,
                        onOpenInventoryFiltered = onOpenInventoryFiltered,
                    )
                }

                item {
                    Text(
                        text = "إدارة المستودع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
                            WarehouseDashboardActionCard(
                                title = "معرض منتجاتي",
                                icon = Icons.Outlined.Inventory2,
                                enabled = true,
                                onClick = onManageInventory,
                                modifier = Modifier.weight(1f),
                            )
                            WarehouseDashboardActionCard(
                                title = "إضافة منتج",
                                icon = Icons.Outlined.AddBox,
                                enabled = true,
                                onClick = onAddProduct,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
                            WarehouseDashboardActionCard(
                                title = "طلبات الصيدليات",
                                icon = Icons.Outlined.Assignment,
                                onClick = onOpenRequests,
                                badgeCount = uiState.stats?.incomingRequestsCount ?: 0,
                                modifier = Modifier.weight(1f),
                            )
                            WarehouseDashboardActionCard(
                                title = "الإشعارات",
                                icon = Icons.Outlined.NotificationsNone,
                                onClick = onOpenNotifications,
                                badgeCount = uiState.stats?.unreadNotificationsCount ?: 0,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        
                        Spacer(Modifier.height(d.spaceS))

                        WarehouseDashboardListAction(
                            title = "الملف الشخصي",
                            icon = Icons.Outlined.Person,
                            onClick = onOpenProfile,
                        )
                    }
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = PharmaBlue50,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                PharmaStatusChip(
                    label = if (hasLinkedWarehouse) "مرتبط" else "غير مرتبط",
                    tone = if (hasLinkedWarehouse) StatusTone.Success else StatusTone.Urgent,
                )
                
                Spacer(Modifier.height(d.spaceXS))

                Text(
                    text = "لوحة المستودع",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumPrimary,
                )
                Text(
                    text = warehouseName.ifBlank { "مستودع الأميرة الحديثة" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                
                Text(
                    text = "أدر معرض منتجاتك، استقبل طلبات الصيدليات، وتابع حالة حسابك من مكان واحد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = PharmaNeutral600,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.2
                )
            }

            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = PremiumPrimary,
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatsGrid(
    stats: WarehouseDashboardStats?,
    isLoading: Boolean,
    onOpenInventoryFiltered: (filter: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val loadingValue = "..."
    
    val items = listOf(
        Triple("المنتجات", stats?.productsCount?.toString() ?: loadingValue, null),
        Triple("الموردون", stats?.suppliersCount?.toString() ?: loadingValue, null),
        Triple("بانتظار موافقة الصيدلية", stats?.quotePendingCount?.toString() ?: loadingValue, null),
        Triple("مخزون منخفض", stats?.lowStockCount?.toString() ?: loadingValue, "LOW_STOCK"),
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
                rowItems.forEach { (label, value, filter) ->
                    val isLowStock = label == "مخزون منخفض"
                    DashboardStatCard(
                        label = label,
                        value = value,
                        valueColor = if (isLowStock && value != loadingValue && (value.toIntOrNull() ?: 0) > 0) PremiumUrgent else PremiumPrimary,
                        onClick = { onOpenInventoryFiltered(filter) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        
        // Final wide stat
        DashboardStatCard(
            label = "سيغلق قريباً",
            value = stats?.closingSoonCount?.toString() ?: loadingValue,
            valueColor = PharmaNeutral900,
            onClick = { onOpenInventoryFiltered("HIDDEN") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    valueColor: Color = PremiumPrimary,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = PharmaBlue50,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
                textAlign = TextAlign.Center
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
    val suppliersCount: Int,
    val closingSoonCount: Int,
    val unreadNotificationsCount: Int = 0,
)

@HiltViewModel
class WarehouseDashboardViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WarehouseDashboardUiState())
    val uiState: StateFlow<WarehouseDashboardUiState> = _uiState.asStateFlow()
    private var requestJob: Job? = null
    private var notificationsJob: Job? = null

    fun load(warehouseId: String) {
        requestJob?.cancel()
        notificationsJob?.cancel()
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
                            suppliersCount = 12, // Placeholder
                            closingSoonCount = inventory.count { item -> !item.isVisible || !item.isActive }, // Map hidden/inactive here for now
                            unreadNotificationsCount = currentStats?.unreadNotificationsCount ?: 0,
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
                                suppliersCount = 12,
                                closingSoonCount = 0,
                                unreadNotificationsCount = 0,
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

        notificationsJob = viewModelScope.launch {
            pharmaRepository.observeNotifications().collect { notifications ->
                val unreadCount = notifications.count { !it.read }
                _uiState.update { current ->
                    val currentStats = current.stats
                    current.copy(
                        stats = if (currentStats == null) {
                            WarehouseDashboardStats(
                                productsCount = 0,
                                incomingRequestsCount = 0,
                                quotePendingCount = 0,
                                lowStockCount = 0,
                                suppliersCount = 12,
                                closingSoonCount = 0,
                                unreadNotificationsCount = unreadCount,
                            )
                        } else {
                            currentStats.copy(
                                unreadNotificationsCount = unreadCount,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseDashboardActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeCount: Int = 0,
) {
    val d = MaterialTheme.dimens
    val contentAlpha = if (enabled) 1f else 0.48f

    Surface(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        shape = RoundedCornerShape(d.radiusXL),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(d.radiusL),
                    color = PharmaBlue50.copy(alpha = contentAlpha),
                    contentColor = PremiumPrimary.copy(alpha = contentAlpha),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                if (badgeCount > 0) {
                    val badgeText = if (badgeCount > 99) "99+" else badgeCount.toString()
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp),
                        color = PremiumUrgent,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900.copy(alpha = contentAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WarehouseDashboardListAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceM, vertical = d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = PharmaNeutral400,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = PharmaBlue50,
                contentColor = PremiumPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarehouseDashboardPreview() {
    com.pharmalink.designsystem.theme.PharmaTheme {
        WarehouseDashboardContent(
            warehouseId = "w123",
            warehouseName = "مستودع الأمل للأدوية",
            uiState = WarehouseDashboardUiState(
                isLoading = false,
                stats = WarehouseDashboardStats(
                    productsCount = 150,
                    incomingRequestsCount = 5,
                    quotePendingCount = 2,
                    lowStockCount = 12,
                    suppliersCount = 8,
                    closingSoonCount = 0
                )
            ),
            onManageInventory = {},
            onOpenInventoryFiltered = {},
            onAddProduct = {},
            onOpenRequests = {},
            onOpenNotifications = {},
            onOpenProfile = {},
            onBack = {}
        )
    }
}
