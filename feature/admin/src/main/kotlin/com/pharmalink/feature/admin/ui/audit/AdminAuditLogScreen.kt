package com.pharmalink.feature.admin.ui.audit

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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumUrgent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pharmalink.designsystem.theme.PharmaWarning
import com.pharmalink.designsystem.theme.StatusActive
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.admin.R
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarButton
import java.time.LocalDate

@Composable
fun AdminAuditLogScreen(
    onOpenLogDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onShowAdminMenu: () -> Unit = {},
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminAuditLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminAuditLogEffect.NavigateToDetail -> onOpenLogDetail(effect.logId)
            is AdminAuditLogEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AdminAuditLogContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToProfile = onNavigateToProfile,
        onShowAdminMenu = onShowAdminMenu,
        profileImageUrl = profileImageUrl,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAuditLogContent(
    state: AdminAuditLogUiState,
    onAction: (AdminAuditLogAction) -> Unit,
    onNavigateToProfile: () -> Unit,
    onShowAdminMenu: () -> Unit,
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
                            text = "سجل التدقيق",
                            style = MaterialTheme.typography.titleLarge,
                            color = PharmaBlue900,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(d.spaceM))
                            AdminProfileAvatarButton(
                                profileImageUrl = profileImageUrl,
                                contentDescription = stringResource(R.string.admin_profile_cd),
                                onClick = onNavigateToProfile,
                                size = 36.dp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onShowAdminMenu) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
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
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminAuditLogAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.logs.isEmpty() -> EmptyContent(modifier = Modifier.padding(padding))
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
        PharmaSkeletonLine(heightDp = 120f)
        repeat(5) {
            PharmaSkeletonLine(heightDp = 100f)
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
            title = "خطأ في تحميل السجل",
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = "إعادة المحاولة",
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
            title = "لا توجد سجلات",
            subtitle = "لم يتم العثور على أي سجلات تدقيق",
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    state: AdminAuditLogUiState,
    onAction: (AdminAuditLogAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        // Date Filter Card
        item {
            DateFilterCard(
                onFilterClick = { onAction(AdminAuditLogAction.OnFilterClicked) },
            )
        }

        // Grouped Logs
        state.logs.forEach { group ->
            item(key = "header_${group.dateLabel}") {
                Text(
                    text = group.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900,
                    modifier = Modifier.padding(top = d.spaceM),
                )
            }

            items(
                items = group.logs,
                key = { it.id },
            ) { log ->
                AuditLogCard(
                    log = log,
                    onClick = { onAction(AdminAuditLogAction.OnLogClicked(log.id)) },
                )
            }
        }
    }
}

@Composable
private fun DateFilterCard(
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceM)
        ) {
            // Search Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusM),
                color = PharmaNeutral100.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.spaceM, vertical = d.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PharmaNeutral400,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "البحث في السجلات...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PharmaNeutral400
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM)
            ) {
                // Filter Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(onClick = onFilterClick),
                    shape = RoundedCornerShape(d.radiusM),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = PharmaNeutral600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(d.spaceS))
                        Text(
                            text = "تصفية",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PharmaNeutral600
                        )
                    }
                }

                // Export Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { /* Handle Export */ },
                    shape = RoundedCornerShape(d.radiusM),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = PharmaNeutral600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(d.spaceS))
                        Text(
                            text = "تصدير",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PharmaNeutral600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(
    log: AuditLogItemModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    val statusColor = when (log.borderColor) {
        AuditLogBorderColor.GREEN -> StatusActive
        AuditLogBorderColor.BLUE -> PremiumPrimary
        AuditLogBorderColor.RED -> PremiumUrgent
        AuditLogBorderColor.ORANGE -> PharmaWarning
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = log.actionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900
                    )
                    Text(
                        text = log.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = PharmaNeutral600
                    )
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (log.borderColor) {
                                AuditLogBorderColor.GREEN -> Icons.Outlined.CheckCircle
                                AuditLogBorderColor.RED -> Icons.Outlined.ErrorOutline
                                AuditLogBorderColor.BLUE -> Icons.Outlined.Edit
                                AuditLogBorderColor.ORANGE -> Icons.Outlined.HistoryEdu
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infoIcon = if (log.iconType == AuditLogIconType.SECURITY) Icons.Default.Security else Icons.Outlined.Person
                    val infoText = if (log.iconType == AuditLogIconType.SECURITY && log.ipAddress != null) 
                        "IP: ${log.ipAddress}" else "بواسطة: ${log.adminName}"
                    
                    Icon(
                        imageVector = infoIcon,
                        contentDescription = null,
                        tint = PharmaNeutral400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.labelSmall,
                        color = PharmaNeutral600
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = PharmaNeutral400,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${log.relativeTime} • ${log.exactTimestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PharmaNeutral600
                    )
                }
            }

            Text(
                text = "التفاصيل",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral600,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminAuditLogScreen() {
    PharmaTheme {
        AdminAuditLogContent(
            state = AdminAuditLogUiState(
                logs = listOf(
                    AuditLogGroup(
                        dateLabel = "اليوم",
                        logs = listOf(
                            AuditLogItemModel(
                                id = "1",
                                iconType = AuditLogIconType.CREATE,
                                actionTitle = "إضافة دواء جديد",
                                description = "تم إضافة دواء Amoxicillin 500mg بنجاح",
                                relativeTime = "اليوم",
                                adminName = "د. سارة أحمد",
                                ipAddress = null,
                                statusChip = "نجح",
                                exactTimestamp = "12:45 صباحاً",
                                borderColor = AuditLogBorderColor.GREEN,
                            ),
                            AuditLogItemModel(
                                id = "2",
                                iconType = AuditLogIconType.SECURITY,
                                actionTitle = "محاولة تسجيل دخول فاشلة",
                                description = "admin@pharmanet.com",
                                relativeTime = "أمس",
                                adminName = "نظام الحماية",
                                ipAddress = "192.168.1.105",
                                statusChip = "فشل",
                                exactTimestamp = "09:15 صباحاً",
                                borderColor = AuditLogBorderColor.RED,
                            ),
                        ),
                    ),
                    AuditLogGroup(
                        dateLabel = "أمس",
                        logs = listOf(
                            AuditLogItemModel(
                                id = "3",
                                iconType = AuditLogIconType.DELETE,
                                actionTitle = "حذف مستخدم",
                                description = "قام النظام بحذف حساب المستخدم بسبب عدم النشاط",
                                relativeTime = "أمس",
                                adminName = "نظام الإدارة",
                                ipAddress = null,
                                statusChip = "فشل",
                                exactTimestamp = "AM 07:30",
                                borderColor = AuditLogBorderColor.RED,
                            ),
                        ),
                    ),
                ),
            ),
            onAction = {},
            onNavigateToProfile = {},
            onShowAdminMenu = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
