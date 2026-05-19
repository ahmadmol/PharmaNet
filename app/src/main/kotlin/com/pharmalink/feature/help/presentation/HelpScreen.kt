package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

data class HelpCategory(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onOpenCompliance: () -> Unit = {},
    onOpenContactUs: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HelpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    var expandedQuestion by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PharmaScreenScaffold(
            title = stringResource(R.string.help_center_title),
            onBack = onBack,
            navigationContentDescription = stringResource(R.string.common_back),
            modifier = modifier,
            actions = {
                TextButton(onClick = onOpenCompliance) {
                    Text(stringResource(R.string.help_compliance_action))
                }
            },
        ) {
            PharmaScreenState(
                screenState = state.screenState,
                loading = PharmaStateSpec(
                    title = stringResource(R.string.help_center_title),
                    subtitle = stringResource(R.string.help_loading_subtitle),
                    tone = PharmaStateTone.Loading,
                ),
                empty = PharmaStateSpec(
                    title = stringResource(R.string.help_center_title),
                    subtitle = stringResource(R.string.help_error_subtitle),
                ),
                error = PharmaStateSpec(
                    title = stringResource(R.string.help_center_title),
                    subtitle = stringResource(R.string.help_error_subtitle),
                    tone = PharmaStateTone.Error,
                ),
                offline = PharmaStateSpec(
                    title = stringResource(R.string.help_center_title),
                    subtitle = stringResource(R.string.help_error_subtitle),
                    tone = PharmaStateTone.Offline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL, vertical = d.spaceXL),
            ) { content ->
                HelpContent(
                    content = content,
                    expandedQuestion = expandedQuestion,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onQuestionToggle = { question ->
                        expandedQuestion = if (expandedQuestion == question) null else question
                    },
                    onOpenCompliance = onOpenCompliance,
                    onOpenContactUs = onOpenContactUs,
                )
            }
        }
    }
}

@Composable
private fun HelpContent(
    content: HelpContent,
    expandedQuestion: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onQuestionToggle: (String) -> Unit,
    onOpenCompliance: () -> Unit,
    onOpenContactUs: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val context = LocalContext.current
    val categories = remember(content) {
        listOf(
            HelpCategory(
                icon = Icons.Outlined.Description,
                title = context.getString(R.string.help_category_user_guide_title),
                subtitle = context.getString(R.string.help_category_user_guide_subtitle),
            ),
            HelpCategory(
                icon = Icons.Outlined.Shield,
                title = context.getString(R.string.help_category_compliance_title),
                subtitle = context.getString(R.string.help_category_compliance_subtitle),
            ),
            HelpCategory(
                icon = Icons.Outlined.Settings,
                title = context.getString(R.string.help_category_account_settings_title),
                subtitle = context.getString(R.string.help_category_account_settings_subtitle),
            ),
            HelpCategory(
                icon = Icons.Outlined.ContactSupport,
                title = context.getString(R.string.help_category_quick_contact_title),
                subtitle = context.getString(R.string.help_category_quick_contact_subtitle),
            ),
        )
    }
    val filteredGuides = remember(content.guides, searchQuery) {
        if (searchQuery.isBlank()) {
            content.guides
        } else {
            content.guides.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val filteredFaq = remember(content.faq, searchQuery) {
        if (searchQuery.isBlank()) {
            content.faq
        } else {
            content.faq.filter {
                it.question.contains(searchQuery, ignoreCase = true) ||
                    it.answer.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            HelpHeroCard(
                content = content,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
            )
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.help_browse_topics_title),
                subtitle = stringResource(R.string.help_browse_topics_subtitle),
            )
        }
        item {
            HelpCategoryGrid(categories = categories)
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.help_channels_title),
                subtitle = stringResource(R.string.help_channels_subtitle),
            )
        }
        items(content.channels, key = { it.title }) { channel ->
            SupportChannelCard(channel)
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.help_guides_title),
                subtitle = stringResource(R.string.help_guides_subtitle),
            )
        }
        if (filteredGuides.isEmpty()) {
            item {
                SearchEmptyCard(
                    title = stringResource(R.string.help_search_no_results_title),
                    subtitle = stringResource(R.string.help_search_no_results_subtitle),
                )
            }
        } else {
            items(filteredGuides, key = { it.title }) { guide ->
                HelpInfoCard(
                    icon = Icons.Outlined.Description,
                    title = guide.title,
                    subtitle = guide.description,
                )
            }
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.help_report_issue_steps_title),
                subtitle = stringResource(R.string.help_report_issue_subtitle),
            )
        }
        item {
            ReportIssueCard()
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.help_faq_title_phase3),
                subtitle = stringResource(R.string.help_faq_subtitle),
            )
        }
        if (filteredFaq.isEmpty()) {
            item {
                SearchEmptyCard(
                    title = stringResource(R.string.help_faq_empty_title),
                    subtitle = stringResource(R.string.help_faq_empty_subtitle),
                )
            }
        } else {
            items(filteredFaq, key = { it.question }) { faq ->
                FaqCard(
                    item = faq,
                    expanded = expandedQuestion == faq.question,
                    onToggle = { onQuestionToggle(faq.question) },
                )
            }
        }
        item {
            SupportCtaCard(
                onOpenCompliance = onOpenCompliance,
                onOpenContactUs = onOpenContactUs,
            )
        }
    }
}

@Composable
private fun HelpHeroCard(
    content: HelpContent,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreenHorizontal)
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = content.pharmacyName.ifBlank { "PharmaLink" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(d.radiusXL),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                placeholder = {
                    Text(stringResource(R.string.help_search_placeholder))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
                ),
            )
            Text(
                text = stringResource(R.string.help_search_helper_text),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                PharmaOutlinedTile(
                    title = stringResource(R.string.help_metric_unread),
                    value = content.unreadNotifications.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.help_metric_compliance),
                    value = content.complianceAttentionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.help_metric_channels),
                    value = content.channels.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HelpCategoryGrid(categories: List<HelpCategory>) {
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                rowItems.forEach { item ->
                    HelpCategoryCard(
                        category = item,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HelpCategoryCard(
    category: HelpCategory,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(20.dp),
                )
            }
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = category.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchEmptyCard(
    title: String,
    subtitle: String,
) {
    HelpInfoCard(
        icon = Icons.Outlined.HelpOutline,
        title = title,
        subtitle = subtitle,
        badgeLabel = stringResource(R.string.help_badge_tip),
        badgeTone = StatusTone.Neutral,
    )
}

@Composable
private fun SupportChannelCard(channel: SupportChannel) {
    HelpInfoCard(
        icon = channel.icon(),
        title = channel.title,
        subtitle = "${channel.detail}\n${channel.availability}\n${channel.guidance}",
        badgeLabel = channel.badgeLabel(),
        badgeTone = when (channel.type) {
            SupportChannelType.Operations -> StatusTone.Warning
            SupportChannelType.Email -> StatusTone.Neutral
            SupportChannelType.Compliance -> StatusTone.Success
        },
    )
}

@Composable
private fun ReportIssueCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            IssueStep(text = stringResource(R.string.help_report_issue_step_1))
            IssueStep(text = stringResource(R.string.help_report_issue_step_2))
            IssueStep(text = stringResource(R.string.help_report_issue_step_3))
        }
    }
}

@Composable
private fun IssueStep(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FaqCard(
    item: HelpFaqItem,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) {
                        stringResource(R.string.help_collapse_answer)
                    } else {
                        stringResource(R.string.help_expand_answer)
                    },
                )
            }
            if (expanded) {
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SupportCtaCard(
    onOpenCompliance: () -> Unit,
    onOpenContactUs: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.primaryHorizontal)
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.help_support_cta_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.help_support_cta_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Button(
                    onClick = onOpenContactUs,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(stringResource(R.string.help_support_cta_contact_button))
                }
                OutlinedButton(
                    onClick = onOpenCompliance,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text(stringResource(R.string.help_support_cta_help_center_button))
                }
            }
        }
    }
}

@Composable
private fun HelpInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeLabel: String? = null,
    badgeTone: StatusTone = StatusTone.Neutral,
) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (badgeLabel != null) {
                PharmaStatusChip(
                    label = badgeLabel,
                    tone = badgeTone,
                )
            }
        }
    }
}

@Composable
private fun SupportChannel.icon(): ImageVector = when (type) {
    SupportChannelType.Operations -> Icons.Outlined.Phone
    SupportChannelType.Email -> Icons.Outlined.Description
    SupportChannelType.Compliance -> Icons.Outlined.Shield
}

@Composable
private fun SupportChannel.badgeLabel(): String = when (type) {
    SupportChannelType.Operations -> stringResource(R.string.help_channel_badge_operations)
    SupportChannelType.Email -> stringResource(R.string.help_channel_badge_email)
    SupportChannelType.Compliance -> stringResource(R.string.help_channel_badge_compliance)
}
