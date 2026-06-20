package com.pharmalink.feature.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaNeutral200
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PremiumPrimary
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmalink.feature.admin.R

enum class AdminActionDestination {
    USERS,
    PHARMACIES,
    WAREHOUSES,
    ORDERS,
    AUDIT_LOG,
    NOTIFICATIONS,
    PROFILE,
}

private data class AdminActionItem(
    val destination: AdminActionDestination,
    val titleRes: Int,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionsBottomSheet(
    onDismiss: () -> Unit,
    profileImageUrl: String? = null,
    onActionClick: (AdminActionDestination) -> Unit,
) {
    val items = listOf(
        AdminActionItem(AdminActionDestination.USERS, R.string.admin_action_users, Icons.Outlined.SupervisorAccount),
        AdminActionItem(AdminActionDestination.PHARMACIES, R.string.admin_action_pharmacies, Icons.Outlined.LocalPharmacy),
        AdminActionItem(AdminActionDestination.WAREHOUSES, R.string.admin_action_warehouses, Icons.Outlined.Warehouse),
        AdminActionItem(AdminActionDestination.ORDERS, R.string.admin_action_orders, Icons.Outlined.ShoppingCart),
        AdminActionItem(AdminActionDestination.AUDIT_LOG, R.string.admin_action_audit_log, Icons.Outlined.HistoryEdu),
        AdminActionItem(AdminActionDestination.NOTIFICATIONS, R.string.admin_action_notifications, Icons.Outlined.NotificationsNone),
        AdminActionItem(AdminActionDestination.PROFILE, R.string.admin_action_profile, Icons.Outlined.Person),
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(PharmaNeutral200, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = PharmaNeutral600
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "د. أحمد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PharmaNeutral900
                            )
                            Text(
                                text = "لوحة التحكم",
                                style = MaterialTheme.typography.labelSmall,
                                color = PharmaNeutral600
                            )
                        }
                        AdminProfileAvatarIcon(
                            profileImageUrl = profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, PharmaBlue50, CircleShape),
                            fallbackTint = PremiumPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onActionClick(item.destination)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = PharmaNeutral400,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PharmaNeutral900,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(PharmaBlue50, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = PremiumPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
