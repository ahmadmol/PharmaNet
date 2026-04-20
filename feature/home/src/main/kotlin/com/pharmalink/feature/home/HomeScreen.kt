package com.pharmalink.feature.home

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.feature.home.R

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToFeaturedWarehouses: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> HomeLoadingState()
        is HomeUiState.Success -> {
            HomeContent(
                uiState = state,
                onNavigateToOrders = onNavigateToOrders,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToWarehouses = onNavigateToWarehouses,
                onNavigateToFeaturedWarehouses = onNavigateToFeaturedWarehouses,
                onNavigateToCreateRequest = onNavigateToCreateRequest,
            )
        }


        is HomeUiState.Error -> {
            HomeErrorState(
                message = state.message,
                onRetry = viewModel::loadHomeData,
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState.Success,
    onNavigateToOrders: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToFeaturedWarehouses: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
            contentPadding = PaddingValues(
                start = d.spaceL,
                top = d.spaceM,
                end = d.spaceL,
                bottom = d.spaceXL,
            ),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            item {
                HomeHeader(
                    userName = uiState.userName,
                    onProfileClick = onNavigateToProfile,
                    onNotificationsClick = onNavigateToNotifications,
                )
            }
            item { HomeSearchCard(onSearchClick = onNavigateToWarehouses) }
            item { SectionHeader(title = "لمحة اليوم") }
            item {
                HomeStatsSection(
                    stats = uiState.stats,
                    isLoading = uiState.isLoadingMore,
                )
            }
            if (!uiState.alertMessage.isNullOrBlank()) {
                item {
                    HomeAlertCard(
                        message = uiState.alertMessage,
                        onReviewClick = onNavigateToWarehouses,
                    )
                }
            }
            item {
                QuickActionsSection(
                    canAddMedicine = uiState.canAddMedicine,
                    onQuickRequestClick = onNavigateToCreateRequest,
                    onChatClick = onNavigateToProfile,
                    onReportsClick = onNavigateToOrders,
                )
            }
            item {
                DashboardPanel {
                    RecentActivitiesSection(
                        onViewAll = onNavigateToOrders,
                    )
                    Spacer(Modifier.height(d.spaceXL))
                    FeaturedWarehousesSection(
                        warehouses = uiState.featuredWarehouses,
                        onViewAll = onNavigateToFeaturedWarehouses,
                    )
                }
            }
        }
    }
}


@Composable
private fun HomeHeader(
    userName: String,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onProfileClick,
                ),
            shape = CircleShape,
            color = PharmaBlue500,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = d.cardElevation,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LocalPharmacy, contentDescription = null, modifier = Modifier.size(d.iconM))
            }
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text("طاب يومك", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "أهلاً، $userName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }

        Box {
            HeaderIconButton(Icons.Outlined.Notifications, stringResource(R.string.home_alerts_title), onNotificationsClick)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .background(PremiumUrgent, CircleShape),
            )
        }
    }
}

@Composable
private fun HomeSearchCard(onSearchClick: () -> Unit) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onSearchClick,
                )
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Surface(shape = RoundedCornerShape(d.radiusM), color = PharmaBlue50, contentColor = PharmaBlue500) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(d.iconS),
                )
            }
        }
    }
}

@Composable
private fun HomeStatsSection(
    stats: HomeStats?,
    isLoading: Boolean
) {
    val d = MaterialTheme.dimens
    
    val displayStats = remember(stats) {
        listOf(
            StatItem("الطلبات اليوم", stats?.requestsTodayCount?.toString() ?: "—", "", stats?.requestsTodayTrend ?: ""),
            StatItem("المخزون الكلي", stats?.totalInventoryCount?.toString() ?: "—", "", stats?.totalInventoryUnit.orEmpty()),
            StatItem("المبيعات", stats?.weeklySalesAmount ?: "—", "", stats?.weeklySalesTrend.orEmpty()),
        )
    }


    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        displayStats.chunked(2).forEach { rowStats ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                rowStats.forEach { stat ->
                    val index = displayStats.indexOf(stat)
                    HomeStatCard(
                        stat = stat,
                        isPrimary = index == 0,
                        isAlert = index == 1,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowStats.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class StatItem(
    val label: String,
    val value: String,
    val indicator: String,
    val details: String
)

@Composable
private fun HomeStatCard(
    stat: StatItem,
    isPrimary: Boolean,
    isAlert: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val containerColor = if (isPrimary) PharmaBlue500 else MaterialTheme.colorScheme.surface
    val contentColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val accentColor = if (isAlert) PremiumUrgent else if (isPrimary) MaterialTheme.colorScheme.onPrimary else PharmaBlue500

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = d.cardElevation),
        border = if (isPrimary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = if (isAlert) Icons.Outlined.Inventory2 else Icons.Outlined.Warehouse,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(d.iconM),
                )
                if (stat.indicator.isNotBlank() || stat.details.isNotBlank()) {
                    Text(
                        text = "${stat.indicator}${stat.details}".trim(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPrimary) contentColor.copy(alpha = 0.86f) else accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(stat.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentColor)
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeAlertCard(message: String, onReviewClick: () -> Unit) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = PremiumUrgent,
        contentColor = Color.White,
        shadowElevation = d.cardElevation,
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(d.spaceL)) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.TopStart).size(96.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(d.spaceS),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(d.iconS))
                    Text("تنبيه مخزون", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                }
                Text("مراجعة مطلوبة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Start)
                Spacer(Modifier.height(d.spaceS))
                StitchButton(
                    onClick = onReviewClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceM),
                ) {
                    Text("مراجعة النواقص الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    canAddMedicine: Boolean,
    onQuickRequestClick: () -> Unit,
    onChatClick: () -> Unit,
    onReportsClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    val actions = remember(canAddMedicine, onQuickRequestClick, onChatClick, onReportsClick) {
        buildList {
            add(QuickActionItem(icon = Icons.Filled.FastRewind, text = "طلب سريع", onClick = onQuickRequestClick))
            if (canAddMedicine) {
                add(
                    QuickActionItem(
                        icon = Icons.Filled.Add,
                        text = "إضافة دواء",
                        onClick = null,
                        enabled = false,
                    ),
                )
            }
            add(QuickActionItem(icon = Icons.Filled.Chat, text = "تواصل", onClick = onChatClick))
            add(QuickActionItem(icon = Icons.Filled.Newspaper, text = "تقارير", onClick = onReportsClick))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
        horizontalAlignment = Alignment.Start,
    ) {
        SectionHeader(title = stringResource(R.string.quick_actions_title))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            actions.forEachIndexed { index, action ->
                QuickActionItemCard(
                    action = action,
                    modifier = Modifier.weight(1f),
                    isPrimary = index == 0,
                )
            }
            if (actions.size < 4) {
                repeat(4 - actions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuickActionItem(
    val icon: ImageVector,
    val text: String,
    val onClick: (() -> Unit)?,
    val enabled: Boolean = true,
)

@Composable
private fun QuickActionItemCard(
    action: QuickActionItem,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val iconContainer = if (isPrimary) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val iconColor = if (isPrimary) MaterialTheme.colorScheme.onSecondaryContainer else PharmaBlue500
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .graphicsLayer { alpha = if (action.enabled) 1f else 0.48f }
            .clip(RoundedCornerShape(d.radiusL))

            .clickable(
                enabled = action.enabled && action.onClick != null,
                interactionSource = interactionSource,
                indication = if (action.enabled) ripple() else null,
                onClick = { action.onClick?.invoke() },
            )
            .padding(vertical = d.spaceXS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = iconContainer, contentColor = iconColor, shadowElevation = if (isPrimary) d.cardElevation else 1.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(d.iconM))
            }
        }
        Text(
            text = action.text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun DashboardPanel(content: @Composable () -> Unit) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = d.radiusXXL, bottomEnd = d.radiusXXL),
        color = PharmaNeutral100,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceL)) {
            content()
        }
    }
}

@Composable
private fun RecentActivitiesSection(onViewAll: () -> Unit) {
    val d = MaterialTheme.dimens

    val activities = listOf(
        ActivityItem("تمت الموافقة على طلب جديد", "طلب رقم #123456789", "منذ 5 دقائق", Icons.Filled.Info),
        ActivityItem("تم تحديث حالة المخزون", "المستودع الرئيسي", "منذ ساعة", Icons.Filled.Info),
        ActivityItem("تمت معالجة فاتورة", "فاتورة رقم #987654", "أمس", Icons.Filled.Info),
    )

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
        SectionHeader(title = stringResource(R.string.recent_activity_title), actionText = "عرض الكل", onActionClick = onViewAll)
        activities.forEach { activity -> ActivityRow(activity = activity) }
    }
}

private data class ActivityItem(
    val title: String,
    val subtitle: String,
    val time: String,
    val icon: ImageVector
)

@Composable
private fun ActivityRow(activity: ActivityItem) {
    val d = MaterialTheme.dimens

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(d.radiusXL), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(d.spaceM), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(d.radiusL), color = PharmaBlue50, contentColor = PharmaBlue500) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(activity.icon, contentDescription = null, modifier = Modifier.size(d.iconS))
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(activity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${activity.subtitle} - ${activity.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(d.iconS))
        }
    }
}

@Composable
private fun FeaturedWarehousesSection(warehouses: List<Warehouse>, onViewAll: () -> Unit) {
    val d = MaterialTheme.dimens

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
        SectionHeader(
            title = stringResource(R.string.featured_warehouses_title),
            actionText = stringResource(R.string.featured_warehouses_action),
            onActionClick = onViewAll,
        )
        if (warehouses.isEmpty()) {
            EmptyCard(text = "لا توجد مستودعات مميزة حالياً")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(d.spaceM), contentPadding = PaddingValues(horizontal = d.spaceXXS)) {
                itemsIndexed(warehouses) { index, warehouse ->
                    WarehouseFeatureCard(warehouse = warehouse, isPrimary = index == 0)
                }
            }
        }
    }
}

@Composable
private fun WarehouseFeatureCard(warehouse: Warehouse, isPrimary: Boolean) {
    val d = MaterialTheme.dimens
    val statusColor = if (warehouse.inStockPercent > 20) PharmaSuccess else PremiumUrgent
    val statusText = if (warehouse.inStockPercent > 20) "متاح" else "مخزون منخفض"
    val containerColor = if (isPrimary) MaterialTheme.colorScheme.surface else Color.White

    Surface(modifier = Modifier.width(178.dp), shape = RoundedCornerShape(d.radiusXXL), color = containerColor, shadowElevation = d.cardElevation) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.Start,
        ) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = PharmaBlue500, contentColor = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Warehouse, contentDescription = null, modifier = Modifier.size(d.iconS))
                }
            }
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
            Text(
                text = listOf(warehouse.city, warehouse.district).filter { it.isNotBlank() }.joinToString("، "),
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
            Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.14f), contentColor = statusColor) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaBlue500,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onActionClick,
                ),
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = PharmaNeutral100, contentColor = MaterialTheme.colorScheme.onSurface) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(d.iconS))
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(d.spaceL),
        )
    }
}

@Composable
private fun HomeLoadingState() {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
            contentPadding = PaddingValues(
                start = d.spaceL,
                top = d.spaceM,
                end = d.spaceL,
                bottom = d.spaceXL,
            ),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            item { HomeHeaderPlaceholder() }
            item { HomeSearchCardPlaceholder() }
            item { SectionHeaderPlaceholder() }
            item { HomeStatsSectionPlaceholder() }
            item { HomeAlertCardPlaceholder() }
            item { QuickActionsSectionPlaceholder() }
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceXL)) {
                    RecentActivitiesSectionPlaceholder()
                    FeaturedWarehousesSectionPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun HomeErrorState(message: String, onRetry: () -> Unit) {
    val d = MaterialTheme.dimens

    Box(modifier = Modifier.fillMaxSize().background(ClinicalCanvas).padding(d.spaceL), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(d.radiusXXL), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            Column(
                modifier = Modifier.padding(d.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(d.iconL))
                Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                StitchButton(onClick = onRetry) { Text("إعادة المحاولة") }
            }
        }
    }
}



@Composable
private fun ShimmerAnimation(
    width: Float,
    height: Float,
    brush: Brush,
    shape: RoundedCornerShape = RoundedCornerShape(MaterialTheme.dimens.radiusM)
) {
    Spacer(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .background(brush = brush, shape = shape)
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
private fun Modifier.shimmerEffect(): Modifier {
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
private fun HomeSearchCardPlaceholder() {
    val d = MaterialTheme.dimens
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shimmerEffect(RoundedCornerShape(d.radiusXXL))
    )
}

@Composable
private fun FeaturedWarehousesSectionPlaceholder() {
    val d = MaterialTheme.dimens
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        SectionHeaderPlaceholder()
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .shimmerEffect(RoundedCornerShape(d.radiusL))
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .shimmerEffect(RoundedCornerShape(d.radiusL))
                )
            }
        }
    }
}

@Composable
private fun RecentActivitiesSectionPlaceholder() {
    val d = MaterialTheme.dimens
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        SectionHeaderPlaceholder()
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(d.radiusM))
                        .shimmerEffect()
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Spacer(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(d.spaceXS))
                    Spacer(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .shimmerEffect()
                    )
                }
                Spacer(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun HomeAlertCardPlaceholder() {
    val d = MaterialTheme.dimens
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shimmerEffect(RoundedCornerShape(d.radiusXXL))
    )
}

@Composable
private fun QuickActionsSectionPlaceholder() {
    val d = MaterialTheme.dimens
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
        horizontalAlignment = Alignment.Start,
    ) {
        SectionHeaderPlaceholder()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            repeat(4) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatsSectionPlaceholder() {
    val d = MaterialTheme.dimens
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        repeat(2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                repeat(2) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .shimmerEffect(RoundedCornerShape(d.radiusXXL))
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderPlaceholder() {
    val d = MaterialTheme.dimens
    Spacer(
        modifier = Modifier
            .width(100.dp)
            .height(20.dp)
            .shimmerEffect()
    )
}

@Composable
private fun HomeHeaderPlaceholder() {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Spacer(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(d.spaceXS))
            Spacer(
                modifier = Modifier
                    .width(180.dp)
                    .height(24.dp)
                    .shimmerEffect()
            )
        }
        Spacer(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    StitchTheme {
        HomeScreen(
            onNavigateToHome = {},
            onNavigateToOrders = {},
            onNavigateToNotifications = {},
            onNavigateToProfile = {},
            onNavigateToWarehouses = {},
            onNavigateToFeaturedWarehouses = {},
            onNavigateToCreateRequest = {},
        )
    }
}
