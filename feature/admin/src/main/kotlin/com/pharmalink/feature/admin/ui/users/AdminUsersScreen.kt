package com.pharmalink.feature.admin.ui.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.pharmalink.designsystem.theme.StatusActive
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarButton
import com.pharmalink.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onNavigateToCreateUser: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onShowAdminMenu: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminUsersViewModel = hiltViewModel(),
    editUserViewModel: EditUserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminUsersEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            AdminUsersEffect.ShowAdminMenu -> onShowAdminMenu()
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
        onNavigateToProfile = onNavigateToProfile,
        profileImageUrl = profileImageUrl,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (state.isEditSheetVisible && state.editSheetUserId.isNotBlank()) {
        val accountType = runCatching {
            AccountType.valueOf(state.editSheetAccountType)
        }.getOrElse { AccountType.PUBLIC_USER }

        EditUserBottomSheet(
            userId = state.editSheetUserId,
            fullName = state.editSheetFullName,
            accountType = accountType,
            facilityId = state.editSheetFacilityId,
            isActive = state.editSheetIsActive,
            onDismiss = {
                viewModel.onAction(AdminUsersAction.OnDismissEditSheet)
                viewModel.onAction(AdminUsersAction.OnRefreshTriggered)
            },
            snackbarHostState = snackbarHostState,
            viewModel = editUserViewModel,
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
        // Note: Add User FAB removed - users can self-register
        // Admin can manage existing users through Edit functionality
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
            subtitle = stringResource(R.string.admin_users_empty_subtitle),
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
                            text = { Text(stringResource(R.string.admin_sort_name)) },
                            onClick = {
                                onAction(AdminUsersAction.OnSortByChanged(UserSortBy.NAME))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_date_joined)) },
                            onClick = {
                                onAction(AdminUsersAction.OnSortByChanged(UserSortBy.DATE_JOINED))
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_sort_account_type)) },
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
            Crossfade(
                targetState = state.filterStatus,
                animationSpec = tween(durationMillis = 180),
                label = "admin_users_filter_content",
            ) { selectedFilter ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == UserFilterStatus.ALL,
                            onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.ALL)) },
                            label = { Text(stringResource(R.string.admin_filter_all)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == UserFilterStatus.ACTIVE,
                            onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.ACTIVE)) },
                            label = { Text(stringResource(R.string.admin_filter_active)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == UserFilterStatus.INACTIVE,
                            onClick = { onAction(AdminUsersAction.OnFilterStatusChanged(UserFilterStatus.INACTIVE)) },
                            label = { Text(stringResource(R.string.admin_filter_inactive)) }
                        )
                    }
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
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                    slideInVertically(animationSpec = tween(durationMillis = 180)) { it / 12 },
                modifier = Modifier.animateItem(),
            ) {
                UserCard(
                    user = user,
                    onCardClick = { onNavigateToUserDetail(user.id) },
                    onEditClick = { onAction(AdminUsersAction.OnEditUserClicked(user)) },
                )
            }
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(durationMillis = 450),
        label = "admin_users_stat_progress",
    )

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
                    progress = { animatedProgress },
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
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "admin_user_card_press",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .animateContentSize(animationSpec = tween(durationMillis = 180))
            .clickable(
                interactionSource = interactionSource,
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
                        StatusActive.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    },
                ) {
                    Text(
                        text = stringResource(if (user.isActive) R.string.admin_status_active else R.string.admin_status_inactive),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.isActive) {
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
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PharmaButton(
                    text = stringResource(R.string.admin_users_edit_button),
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = "التعطيل عبر التعديل",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = d.spaceS, vertical = d.spaceM),
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
            onNavigateToProfile = {},
            profileImageUrl = null,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}


