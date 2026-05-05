package com.pharmalink.feature.admin.ui.users

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.feature.admin.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onNavigateToCreateUser: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminUsersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditSheet by remember { mutableStateOf(false) }
    var editUserData by remember { mutableStateOf<AdminUsersEffect.ShowEditUserSheet?>(null) }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminUsersEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is AdminUsersEffect.ShowEditUserSheet -> {
                editUserData = effect
                showEditSheet = true
            }
            is AdminUsersEffect.ShowDeleteConfirmation -> {
                snackbarHostState.showSnackbar("حذف المستخدم: قيد التطوير")
            }
        }
    }

    AdminUsersContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToCreateUser = onNavigateToCreateUser,
        onNavigateToUserDetail = onNavigateToUserDetail,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showEditSheet && editUserData != null) {
        EditUserBottomSheet(
            userId = editUserData!!.userId,
            fullName = editUserData!!.fullName ?: "",
            accountType = editUserData!!.accountType,
            facilityId = editUserData!!.facilityId.orEmpty(),
            isActive = editUserData!!.isActive,
            onDismiss = {
                showEditSheet = false
                editUserData = null
                viewModel.onAction(AdminUsersAction.OnRefreshTriggered)
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUsersContent(
    state: AdminUsersUiState,
    onAction: (AdminUsersAction) -> Unit,
    onNavigateToCreateUser: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
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
                            text = stringResource(R.string.admin_users_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(AdminUsersAction.OnMenuClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.admin_menu_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                )
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateUser,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.admin_add_user_cd),
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminUsersAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.users.isEmpty() -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                state = state,
                onAction = onAction,
                onNavigateToUserDetail = onNavigateToUserDetail,
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
        PharmaSkeletonLine(heightDp = 120f)
        PharmaSkeletonLine(heightDp = 80f)
        repeat(5) {
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
            title = stringResource(R.string.admin_users_error),
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = stringResource(R.string.admin_users_retry),
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
            title = stringResource(R.string.admin_users_empty),
            subtitle = stringResource(R.string.audit_log_no_logs),
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    state: AdminUsersUiState,
    onAction: (AdminUsersAction) -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
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
                SearchField(
                    value = state.searchQuery,
                    onValueChange = { onAction(AdminUsersAction.OnSearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.admin_users_search_placeholder),
                    modifier = Modifier.weight(1f),
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
                                onAction(AdminUsersAction.OnSortByChanged(UserSortBy.NAME))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تاريخ الانضمام") },
                            onClick = {
                                onAction(AdminUsersAction.OnSortByChanged(UserSortBy.DATE_JOINED))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نوع الحساب") },
                            onClick = {
                                onAction(AdminUsersAction.OnSortByChanged(UserSortBy.ACCOUNT_TYPE))
                                sortExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                item {
                    FilterChip(
                        selected = state.filterStatus == UserFilterStatus.ALL,
                        onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.ALL)) },
                        label = { Text("الكل") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterStatus == UserFilterStatus.ACTIVE,
                        onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.ACTIVE)) },
                        label = { Text("نشط") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterStatus == UserFilterStatus.INACTIVE,
                        onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.INACTIVE)) },
                        label = { Text("غير نشط") }
                    )
                }
            }
        }

        // Statistics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                StatCard(
                    title = stringResource(R.string.admin_users_total_label),
                    value = state.totalUsers.toString(),
                    subtitle = stringResource(R.string.admin_monthly_growth, state.monthlyGrowth),
                    modifier = Modifier.weight(1f),
                )
                
                StatCard(
                    title = stringResource(R.string.admin_users_active_label),
                    value = state.activeUsers.toString(),
                    progress = if (state.totalUsers > 0) state.activeUsers.toFloat() / state.totalUsers else 0f,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // User Cards
        items(
            items = state.users,
            key = { it.id },
        ) { user ->
            UserCard(
                user = user,
                onCardClick = { onNavigateToUserDetail(user.id) },
                onEditClick = { onAction(AdminUsersAction.OnEditUserClicked(user)) },
                onDeleteClick = { onAction(AdminUsersAction.OnDeleteUserClicked(user.id)) },
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
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
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevationDp = 2f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserItemModel,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    
                    Column {
                        Text(
                            text = user.fullName ?: user.email,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        
                        Text(
                            text = getAccountTypeLabel(user.accountType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                
                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (user.isActive) {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
                ) {
                    Text(
                        text = stringResource(if (user.isActive) R.string.admin_status_active else R.string.admin_status_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.isActive) {
                            Color(0xFF10B981)
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
            
            // Email
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            // Facility
            if (!user.facilityName.isNullOrEmpty()) {
                Text(
                    text = user.facilityName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            HorizontalDivider()
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PharmaButton(
                    text = stringResource(R.string.admin_users_edit_button),
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                )
                
                Spacer(Modifier.width(d.spaceS))
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.admin_users_delete_button),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun getAccountTypeLabel(accountType: AccountType): String {
    return when (accountType) {
        AccountType.PHARMACY -> stringResource(R.string.admin_account_type_pharmacy)
        AccountType.WAREHOUSE -> stringResource(R.string.admin_account_type_warehouse)
        AccountType.ADMIN -> stringResource(R.string.admin_account_type_admin)
        AccountType.PUBLIC_USER -> stringResource(R.string.admin_account_type_public)
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminUsersScreen() {
    PharmaTheme {
        AdminUsersContent(
            state = AdminUsersUiState(
                users = listOf(
                    UserItemModel(
                        id = "1",
                        fullName = "أحمد محمود",
                        email = "ahmed@pharmanet.com",
                        accountType = AccountType.PHARMACY,
                        facilityName = "صيدلية النهدي - الرياض",
                        isActive = true,
                    ),
                    UserItemModel(
                        id = "2",
                        fullName = "سارة الأحمدي",
                        email = "sara.k@pharmanet.com",
                        accountType = AccountType.WAREHOUSE,
                        facilityName = "مستودع التبريد الشمالي",
                        isActive = false,
                    ),
                ),
                totalUsers = 1284,
                activeUsers = 942,
                monthlyGrowth = 12.5f,
            ),
            onAction = {},
            onNavigateToCreateUser = {},
            onNavigateToUserDetail = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
