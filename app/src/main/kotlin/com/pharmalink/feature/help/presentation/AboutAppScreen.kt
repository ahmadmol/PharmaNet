package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmalink.BuildConfig
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

data class AboutFeature(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

data class AboutValue(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
)

@Composable
fun AboutAppScreen(
    onBack: () -> Unit = {},
    onPrimaryCtaClick: () -> Unit,
    onSecondaryCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PharmaScreenScaffold(
            title = stringResource(R.string.about_title),
            onBack = onBack,
            navigationContentDescription = stringResource(R.string.common_back),
            modifier = modifier,
        ) {
            AboutAppContent(
                onPrimaryCtaClick = onPrimaryCtaClick,
                onSecondaryCtaClick = onSecondaryCtaClick,
            )
        }
    }
}

@Composable
private fun AboutAppContent(
    onPrimaryCtaClick: () -> Unit,
    onSecondaryCtaClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val features = listOf(
        AboutFeature(
            icon = Icons.Outlined.Store,
            titleRes = R.string.about_feature_one_title,
            descriptionRes = R.string.about_feature_one_description,
        ),
        AboutFeature(
            icon = Icons.Outlined.Lock,
            titleRes = R.string.about_feature_two_title,
            descriptionRes = R.string.about_feature_two_description,
        ),
        AboutFeature(
            icon = Icons.Outlined.Language,
            titleRes = R.string.about_feature_three_title,
            descriptionRes = R.string.about_feature_three_description,
        ),
    )
    val values = listOf(
        AboutValue(
            icon = Icons.Outlined.CheckCircleOutline,
            titleRes = R.string.about_value_one_title,
            descriptionRes = R.string.about_value_one_description,
        ),
        AboutValue(
            icon = Icons.Outlined.Info,
            titleRes = R.string.about_value_two_title,
            descriptionRes = R.string.about_value_two_description,
        ),
        AboutValue(
            icon = Icons.Outlined.Language,
            titleRes = R.string.about_value_three_title,
            descriptionRes = R.string.about_value_three_description,
        ),
    )

    LazyColumn(
        contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            AboutHeroCard(
                onPrimaryCtaClick = onPrimaryCtaClick,
                onSecondaryCtaClick = onSecondaryCtaClick,
            )
        }
        item { AboutIllustrationCard() }
        item { AboutVisionCard() }
        item { AboutStatsRow() }

        item {
            PharmaSectionHeader(
                title = stringResource(R.string.about_features_section_title),
                subtitle = stringResource(R.string.about_features_section_subtitle),
            )
        }
        items(features) { feature -> AboutFeatureCard(feature) }

        item {
            PharmaSectionHeader(
                title = stringResource(R.string.about_values_section_title),
                subtitle = stringResource(R.string.about_values_section_subtitle),
            )
        }
        items(values) { value -> AboutValueCard(value) }

        item { AboutCommitmentCard() }
        item { AboutFooterMeta() }
    }
}

@Composable
private fun AboutHeroCard(
    onPrimaryCtaClick: () -> Unit,
    onSecondaryCtaClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = PharmaGradients.primaryDiagonal)
                .padding(d.spaceL),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = stringResource(R.string.about_hero_version_badge, BuildConfig.VERSION_NAME),
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.about_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.about_hero_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.94f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Button(
                        onClick = onPrimaryCtaClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(d.radiusXL),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.about_cta_primary),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = onSecondaryCtaClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(d.radiusXL),
                    ) {
                        Text(
                            text = stringResource(R.string.about_cta_secondary),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutIllustrationCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = PharmaBlue700),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(186.dp)
                .background(brush = PharmaGradients.cardBlue),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(110.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutVisionCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = stringResource(R.string.about_vision_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PharmaBlue700,
            )
            Text(
                text = stringResource(R.string.about_vision_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutStatsRow() {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        StatTile(
            title = stringResource(R.string.about_stat_one_title),
            value = stringResource(R.string.about_stat_one_value),
            modifier = Modifier.weight(1f),
            containerColor = PharmaBlue500,
            contentColor = Color.White,
        )
        StatTile(
            title = stringResource(R.string.about_stat_two_title),
            value = stringResource(R.string.about_stat_two_value),
            modifier = Modifier.weight(1f),
            containerColor = Color(0xFFDFF5FF),
            contentColor = PharmaBlue700,
        )
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = d.spaceL, horizontal = d.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun AboutFeatureCard(feature: AboutFeature) {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = PharmaBlue700.copy(alpha = 0.1f),
                contentColor = PharmaBlue700,
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(feature.descriptionRes),
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutValueCard(value: AboutValue) {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = value.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(value.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(value.descriptionRes),
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutCommitmentCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = stringResource(R.string.about_commitment_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.about_commitment_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutFooterMeta() {
    val d = MaterialTheme.dimens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = d.spaceS, bottom = d.spaceL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceXS),
    ) {
        Text(
            text = stringResource(R.string.about_footer_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.about_footer_meta, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
