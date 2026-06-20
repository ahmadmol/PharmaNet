package com.pharmalink.feature.pharmacy

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.data.dto.NearbyOrderDto
import com.pharmalink.designsystem.theme.ClinicalCanvas
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PharmaBlue800
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral200
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource

@Composable
fun PharmacyRadarScreen(
    viewModel: PharmacyRadarViewModel = hiltViewModel(),
    onNavigateToOrderDetail: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var hasRequestedPermission by remember { mutableStateOf(false) }
    var hasLoadedNearbyOrders by remember { mutableStateOf(false) }

    val locationPermissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        val granted = grantResults.values.any { it }
        if (granted) {
            permissionMessage = null
            isPermanentlyDenied = false
            hasLoadedNearbyOrders = true
            viewModel.refreshNearbyOrders()
        } else {
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity != null && locationPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            isPermanentlyDenied = hasRequestedPermission && !shouldShowRationale
            permissionMessage = if (isPermanentlyDenied) {
                context.getString(R.string.pharmacy_dashboard_error_permission_permanently_denied)
            } else {
                context.getString(R.string.pharmacy_dashboard_error_permission_denied)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLoadedNearbyOrders) {
            hasLoadedNearbyOrders = true
            viewModel.refreshNearbyOrders()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            topBar = {
                RadarTopBar(onProfileClick = {}, onMenuClick = {})
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                RadarContent(
                    uiState = uiState,
                    errorMessage = permissionMessage ?: uiState.errorMessage,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onAllowLocationClick = {
                        if (isPermanentlyDenied) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        } else {
                            hasRequestedPermission = true
                            permissionLauncher.launch(locationPermissions)
                        }
                    },
                    onOpenLocationSettingsClick = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    onRetryLocationClick = {
                        permissionMessage = null
                        isPermanentlyDenied = false
                        hasLoadedNearbyOrders = true
                        viewModel.refreshNearbyOrders()
                    },
                    onNavigateToOrderDetail = onNavigateToOrderDetail,
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarTopBar(
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = PharmaBlue700,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Hamburger on the right in RTL (Start)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                )
            }

            Text(
                text = "PharmaNet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            // Profile on the left in RTL (End)
            IconButton(onClick = onProfileClick) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarContent(
    uiState: PharmacyRadarUiState,
    errorMessage: String?,
    isPermanentlyDenied: Boolean,
    onAllowLocationClick: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    onRetryLocationClick: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    val closestDistance = uiState.nearbyOrders.minOfOrNull { it.distanceKm ?: Double.MAX_VALUE }
        ?.takeIf { it != Double.MAX_VALUE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "الرادار",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaBlue700,
                )
            }
        }

        item {
            RadarHero(closestDistance = closestDistance)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "طلبات قريبة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                }
            } else if (!errorMessage.isNullOrBlank()) {
                val isGpsDisabled = errorMessage == stringResource(R.string.pharmacy_dashboard_error_location_disabled)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onRetryLocationClick) {
                            Text(text = stringResource(R.string.pharmacy_dashboard_retry_location))
                        }
                        TextButton(onClick = if (isGpsDisabled) onOpenLocationSettingsClick else onAllowLocationClick) {
                            Text(
                                text = if (isGpsDisabled) {
                                    stringResource(R.string.pharmacy_dashboard_open_settings)
                                } else if (isPermanentlyDenied) {
                                    stringResource(R.string.pharmacy_dashboard_open_settings)
                                } else {
                                    stringResource(R.string.pharmacy_dashboard_allow_location)
                                },
                            )
                        }
                    }
                }
            } else if (uiState.nearbyOrdersCount == 0) {
                if (uiState.isLocationMissing) {
                    LocationMissingCard()
                } else {
                    EmptyNearbyOrdersCard()
                }
            }
        }

        items(uiState.nearbyOrders, key = { it.id ?: "${it.userName.orEmpty()}_${it.medicineName.orEmpty()}" }) { order ->
            val safeId = order.id?.takeIf { it.isNotBlank() }
            RadarOrderItem(
                order = order,
                onClick = safeId?.let { { onNavigateToOrderDetail(it) } },
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun LocationMissingCard() {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(
                    text = "موقع الصيدلية غير محدد",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "الرادار يحتاج إلى إحداثيات الصيدلية. يرجى تحديث موقع الصيدلية من الملف الشخصي.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(
                onClick = { /* This is handled by navigation to profile from parent */ },
                shape = RoundedCornerShape(d.radiusM),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Text(text = "تحديث الموقع", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyNearbyOrdersCard() {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = "لا توجد طلبات عامة قريبة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "يعرض الرادار فقط طلبات العملاء العامة القريبة. الطلبات المرسلة لصيدليتك تظهر في قائمة طلبات العملاء.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadarHero(closestDistance: Double?) {
    val d = MaterialTheme.dimens
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Distance Badge Top-Left in RTL (End)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(d.spaceL),
                shape = RoundedCornerShape(d.radiusXL),
                color = PharmaBlue50,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PharmaBlue700
                    )
                    Text(
                        text = stringResource(R.string.pharmacy_dashboard_badge_distance, closestDistance ?: 0.0),
                        color = PharmaBlue700,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceL)
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    RadarPulseEffect()
                    
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                        color = PremiumPrimary,
                        shadowElevation = 4.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = DsR.drawable.ic_sydaliti_mark),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }
                
                Text(
                    text = "جاري البحث عن صيدليات متوفرة...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PharmaNeutral600,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RadarPulseEffect() {
    val transition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseAlpha = transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        // Static circles
        Box(
            modifier = Modifier
                .size(140.dp)
                .border(1.dp, PharmaNeutral200, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .border(1.dp, PharmaNeutral100, CircleShape)
        )
        
        // Animated Pulse
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { alpha = pulseAlpha.value }
                .border(2.dp, PremiumPrimary.copy(alpha = 0.4f), CircleShape),
        )
    }
}

@Composable
private fun BadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RadarOrderItem(
    order: NearbyOrderDto,
    onClick: (() -> Unit)?,
) {
    val d = MaterialTheme.dimens
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        RadarOrderItemContent(order = order)
    }
}

@Composable
private fun RadarOrderItemContent(order: NearbyOrderDto) {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(d.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Icon on the right in RTL (Start)
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = PharmaBlue50,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (order.medicineName.orEmpty().contains("حقن") || order.medicineName.orEmpty().contains("إبرة")) {
                        Icons.Default.Vaccines
                    } else if (order.medicineName.orEmpty().contains("مكملات")) {
                        Icons.Default.Medication
                    } else {
                        Icons.Default.LocalPharmacy
                    },
                    contentDescription = null,
                    tint = PremiumPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Info in the middle
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = order.medicineName ?: "طلب عام",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
            Text(
                text = order.userName.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = PharmaNeutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }

        // Distance badge on the left in RTL (End)
        Surface(
            shape = RoundedCornerShape(d.radiusM),
            color = PharmaNeutral100,
        ) {
            Text(
                text = stringResource(R.string.pharmacy_dashboard_distance_badge, order.distanceKm ?: 0.0),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = PharmaNeutral600,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
