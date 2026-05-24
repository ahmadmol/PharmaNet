package com.pharmalink.feature.admin.ui.audit

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
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
    val context = LocalContext.current

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
                            text = "السجل",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onShowAdminMenu) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "القائمة",
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                startDate = state.startDate,
                endDate = state.endDate,
                onStartDateSelected = { date ->
                    onAction(AdminAuditLogAction.OnStartDateSelected(date))
                },
                onEndDateSelected = { date ->
                    onAction(AdminAuditLogAction.OnEndDateSelected(date))
                },
                onFilterClick = { onAction(AdminAuditLogAction.OnFilterClicked) },
            )
        }

        // Grouped Logs
        state.logs.forEach { group ->
            item(key = "header_${group.dateLabel}") {
                Text(
                    text = group.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
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
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        elevationDp = 4f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                // Start Date
                DatePickerField(
                    label = "من تاريخ",
                    date = startDate,
                    onDateSelected = onStartDateSelected,
                    modifier = Modifier.weight(1f),
                )

                // End Date
                DatePickerField(
                    label = "إلى تاريخ",
                    date = endDate,
                    onDateSelected = onEndDateSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Filter Button
                PharmaButton(
                    text = "تصفية",
                    onClick = onFilterClick,
                    modifier = Modifier.weight(1f),
                )

                // Export Button — disabled until backend export RPC is available
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "تصدير (غير متاح)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { showDatePicker = true },
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = date?.toString() ?: label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (date != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("إلغاء")
                }
            }
        ) {
            androidx.compose.material3.DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = label,
                        modifier = Modifier.padding(d.spaceM)
                    )
                }
            )
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

    val borderColor = when (log.borderColor) {
        AuditLogBorderColor.GREEN -> StatusActive
        AuditLogBorderColor.BLUE -> MaterialTheme.colorScheme.primary
        AuditLogBorderColor.RED -> MaterialTheme.colorScheme.error
        AuditLogBorderColor.ORANGE -> PharmaWarning
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = borderColor.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.large,
                )
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            // Icon Circle
            val icon = when (log.iconType) {
                AuditLogIconType.CREATE -> Icons.Default.Add
                AuditLogIconType.UPDATE -> Icons.Default.Edit
                AuditLogIconType.DELETE -> Icons.Default.Delete
                AuditLogIconType.SECURITY -> Icons.Default.Security
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = borderColor.copy(alpha = 0.15f),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = log.actionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ) {
                        Text(
                            text = log.statusChip,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(
                                horizontal = d.spaceS,
                                vertical = d.spaceXS,
                            ),
                        )
                    }
                }

                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = log.relativeTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = log.exactTimestamp,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
                                relativeTime = "منذ 15 دقيقة",
                                statusChip = "نجح",
                                exactTimestamp = "AM 12:45",
                                borderColor = AuditLogBorderColor.GREEN,
                            ),
                            AuditLogItemModel(
                                id = "2",
                                iconType = AuditLogIconType.UPDATE,
                                actionTitle = "تعديل بيانات مستخدم",
                                description = "قام سعود بتعديل صلاحيات أحمد لمدير صيدلية النهدي",
                                relativeTime = "منذ ساعتين",
                                statusChip = "نجح",
                                exactTimestamp = "AM 09:19",
                                borderColor = AuditLogBorderColor.BLUE,
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
