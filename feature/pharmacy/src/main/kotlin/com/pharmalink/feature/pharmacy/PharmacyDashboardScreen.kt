package com.pharmalink.feature.pharmacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.DashboardWelcomeCard
import com.pharmalink.designsystem.R as DsR

private val TealPrimary = Color(0xFF00796B)

@Composable
fun PharmacyDashboardScreen(
    viewModel: PharmacyDashboardViewModel = hiltViewModel(),
    onNavigateToNotifications: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color(0xFFF5F6F7),
            topBar = {
                DashboardTopBar(onNavigateToNotifications = onNavigateToNotifications)
            },
        ) { innerPadding ->
            DashboardContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                isLoading = uiState.isLoading,
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    onNavigateToNotifications: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TealPrimary,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.size(48.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = painterResource(id = DsR.drawable.sydaliti_logo_icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.pharmacy_dashboard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            BadgedBox(badge = { Badge(containerColor = Color.Red.copy(alpha = 0.9f)) }) {
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = stringResource(R.string.pharmacy_dashboard_notifications),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { HeaderSection() }
        item {
            DashboardWelcomeCard(
                title = stringResource(R.string.pharmacy_welcome_title),
                subtitle = stringResource(R.string.pharmacy_welcome_subtitle),
                stats = listOf(
                    stringResource(R.string.pharmacy_welcome_stat_nearby) to "-",
                    stringResource(R.string.pharmacy_welcome_stat_location) to "-",
                    stringResource(R.string.pharmacy_welcome_stat_status) to if (isLoading) {
                        stringResource(R.string.pharmacy_welcome_value_loading)
                    } else {
                        stringResource(R.string.pharmacy_welcome_value_ready)
                    },
                ),
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFFE2F2EF), CircleShape)
                .background(TealPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.LocalPharmacy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.pharmacy_dashboard_greeting),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TealPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "${stringResource(R.string.pharmacy_dashboard_location_prefix)} -",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
        }
    }
}
