package com.pharmalink.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

enum class PharmaStateTone {
    Info,
    Loading,
    Error,
    Success,
    Offline,
    Neutral,
}

@Composable
fun PharmaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val d = MaterialTheme.dimens
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            AssistChip(
                onClick = onAction,
                label = { Text(text = actionLabel) },
            )
        }
    }
}

@Composable
fun PharmaStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    helper: String? = null,
) {
    val d = MaterialTheme.dimens
    ElevatedCardWrapper(
        modifier = modifier,
    ) {
        Column(Modifier.padding(d.spaceL)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(d.spaceXS))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            if (!helper.isNullOrBlank()) {
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PharmaActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = PharmaGradients.primaryHorizontal,
) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(brush, RoundedCornerShape(d.radiusM)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(d.spaceXS))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PharmaStatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    StatusChip(label = label, tone = tone, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmaScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = navigationContentDescription,
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
    )
}

data class PharmaStateSpec(
    val title: String,
    val subtitle: String,
    val tone: PharmaStateTone = PharmaStateTone.Info,
    val actionLabel: String? = null,
)

@Composable
fun PharmaScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PharmaScreenTopBar(
                title = title,
                onBack = onBack,
                navigationContentDescription = navigationContentDescription,
                actions = actions,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content(innerPadding)
        }
    }
}

@Composable
fun <T> PharmaScreenState(
    screenState: ScreenState<T>,
    loading: PharmaStateSpec,
    empty: PharmaStateSpec,
    error: PharmaStateSpec,
    offline: PharmaStateSpec,
    modifier: Modifier = Modifier,
    onEmptyAction: (() -> Unit)? = null,
    onErrorAction: (() -> Unit)? = null,
    onOfflineAction: (() -> Unit)? = onErrorAction,
    content: @Composable (T) -> Unit,
) {
    when (screenState) {
        ScreenState.Loading -> PharmaStateView(
            title = loading.title,
            subtitle = loading.subtitle,
            tone = loading.tone,
            actionLabel = loading.actionLabel,
            onAction = null,
            modifier = modifier,
        )
        is ScreenState.Error -> PharmaStateView(
            title = error.title,
            subtitle = screenState.message ?: error.subtitle,
            tone = error.tone,
            actionLabel = error.actionLabel,
            onAction = onErrorAction,
            modifier = modifier,
        )
        is ScreenState.Offline -> PharmaStateView(
            title = offline.title,
            subtitle = screenState.message ?: offline.subtitle,
            tone = offline.tone,
            actionLabel = offline.actionLabel,
            onAction = onOfflineAction,
            modifier = modifier,
        )
        ScreenState.Empty -> PharmaStateView(
            title = empty.title,
            subtitle = empty.subtitle,
            tone = empty.tone,
            actionLabel = empty.actionLabel,
            onAction = onEmptyAction,
            modifier = modifier,
        )
        is ScreenState.Success -> content(screenState.data)
    }
}

@Composable
fun PharmaStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isError: Boolean = false,
    tone: PharmaStateTone = if (isError) PharmaStateTone.Error else PharmaStateTone.Info,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceXL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (tone) {
                PharmaStateTone.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                }
                else -> {
                    val icon = when (tone) {
                        PharmaStateTone.Error -> Icons.Outlined.ErrorOutline
                        PharmaStateTone.Success -> Icons.Outlined.CheckCircleOutline
                        PharmaStateTone.Offline -> Icons.Outlined.CloudOff
                        else -> Icons.Outlined.Info
                    }
                    val tint = when (tone) {
                        PharmaStateTone.Error -> MaterialTheme.colorScheme.error
                        PharmaStateTone.Success -> Color(0xFF0F766E)
                        PharmaStateTone.Offline -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(d.spaceM))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(d.spaceXS))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(Modifier.height(d.spaceL))
                AssistChip(
                    onClick = onAction,
                    label = { Text(actionLabel) },
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
fun PharmaFilterBar(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun ElevatedCardWrapper(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun PharmaOutlinedTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val d = MaterialTheme.dimens
    OutlinedCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(d.spaceL),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(d.spaceXS))
                    Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
