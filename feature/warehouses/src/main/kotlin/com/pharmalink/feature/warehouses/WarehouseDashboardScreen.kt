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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens

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
) {
    val hasLinkedWarehouse = warehouseId.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
            contentPadding = PaddingValues(MaterialTheme.dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
        ) {
            item {
                WarehouseDashboardHeader(
                    warehouseName = warehouseName,
                    hasLinkedWarehouse = hasLinkedWarehouse,
                )
            }

            if (!hasLinkedWarehouse) {
                item {
                    WarehouseLinkBlockingCard(onOpenProfile = onOpenProfile)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)) {
                    WarehouseDashboardActionCard(
                        title = "معرض منتجاتي / إدارة المخزون",
                        subtitle = if (hasLinkedWarehouse) {
                            "راجع المنتجات والكميات وحالة الظهور للصيدليات."
                        } else {
                            "يتطلب ربط حساب المستودع بمنشأة قبل إدارة المنتجات."
                        },
                        icon = Icons.Outlined.Inventory2,
                        enabled = hasLinkedWarehouse,
                        onClick = onManageInventory,
                    )

                    WarehouseDashboardActionCard(
                        title = "إضافة منتج",
                        subtitle = if (hasLinkedWarehouse) {
                            "أضف صورة ومعلومات المنتج والسعر والكمية."
                        } else {
                            "غير متاح حتى يتم ربط الحساب بمنشأة مستودع."
                        },
                        icon = Icons.Outlined.AddBox,
                        enabled = hasLinkedWarehouse,
                        onClick = onAddProduct,
                    )

                    WarehouseDashboardActionCard(
                        title = "طلبات الصيدليات الواردة",
                        subtitle = "تابع طلبات B2B وقم بالتسعير أو الرفض أو التنفيذ.",
                        icon = Icons.Outlined.Assignment,
                        onClick = onOpenRequests,
                    )

                    WarehouseDashboardActionCard(
                        title = "الإشعارات",
                        subtitle = "افتح التنبيهات المرتبطة بالطلبات وتحديثات الحساب.",
                        icon = Icons.Outlined.NotificationsNone,
                        onClick = onOpenNotifications,
                    )

                    WarehouseDashboardActionCard(
                        title = "حالة الملف الشخصي/الموقع",
                        subtitle = if (hasLinkedWarehouse) {
                            "راجع بيانات الحساب وموقع المستودع."
                        } else {
                            "افتح الملف الشخصي لمراجعة حالة الربط والموقع."
                        },
                        icon = Icons.Outlined.LocationOn,
                        onClick = onOpenProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseDashboardHeader(
    warehouseName: String,
    hasLinkedWarehouse: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        modifier = Modifier.size(d.iconM),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "لوحة المستودع",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = warehouseName.ifBlank { "حساب مستودع" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            PharmaStatusChip(
                label = if (hasLinkedWarehouse) "مرتبط" else "غير مرتبط",
                tone = if (hasLinkedWarehouse) StatusTone.Success else StatusTone.Urgent,
            )
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
                    text = "حساب المستودع غير مرتبط بمنشأة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "إدارة المخزون وإضافة المنتجات متوقفة حتى يتم ربط الحساب بمستودع صالح.",
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
                    text = "الملف",
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
    val contentAlpha = if (enabled) 1f else 0.52f

    Card(
        modifier = modifier
            .fillMaxWidth()
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
    }
}
